package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class QuoteWithdrawalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    private Long userId;

    private Long serviceOrderId;

    private CPFCNPJ customerDocument;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Administrador",
                        new Email("admin-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ADMIN),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
        as(post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId)).andExpect(status().isOk());
    }

    @Test
    void shouldWithdrawTheSentQuoteReleasingTheReservationSoThePartCanBeSwapped() throws Exception {
        Long missing = createPart(5);
        Long similar = createPart(5);
        Long missingItemId = addPart(missing, 3);
        sendQuote().andExpect(status().isOk());

        as(post("/admin/serviceOrders/{id}/reopenQuote", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Peça em falta, trocar pela similar"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"));

        assertThat(reservedOf(missing)).isZero();
        assertThat(lastStatusChange())
                .containsEntry("from_status", "AWAITING_APPROVAL")
                .containsEntry("to_status", "IN_DIAGNOSIS")
                .containsEntry("description", "Peça em falta, trocar pela similar");

        as(delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, missingItemId))
                .andExpect(status().isNoContent());
        addPart(similar, 3);
        sendQuote().andExpect(status().isOk());

        assertThat(reservedOf(missing)).isZero();
        assertThat(reservedOf(similar)).isEqualTo(3);
        assertThat(movementCount()).isZero();
    }

    @Test
    void shouldWithdrawTheSupplementaryQuoteAndGoOnWithTheApprovedServices() throws Exception {
        Long pad = createPart(10);
        Long filter = createPart(10);
        Long serviceItemId = addService();
        addPart(pad, 2);
        sendQuote().andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/startExecution", serviceOrderId)).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/services/{itemId}/start", serviceOrderId, serviceItemId))
                .andExpect(status().isOk());
        addPart(filter, 1);
        as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canProceedWithoutChanges": true}
                                """))
                .andExpect(status().isOk());

        as(post("/admin/serviceOrders/{id}/reopenQuote", serviceOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.canProceedWithoutChanges").doesNotExist());

        assertThat(reservedOf(filter)).isZero();
        assertThat(stockOf(pad)).isEqualTo(8);
        as(post("/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, serviceItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        as(post("/admin/serviceOrders/{id}/complete", serviceOrderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.quoteChangesPendingApproval"));
    }

    @Test
    void shouldLetOnlyTheApprovalOrTheWithdrawalPassWhenTheyHappenTogether() throws Exception {
        Long filter = createPart(5);
        addPart(filter, 3);
        sendQuote().andExpect(status().isOk());
        UUID publicId = jdbcTemplate.queryForObject(
                "SELECT public_id FROM service_order WHERE id = ?", UUID.class, serviceOrderId);
        CountDownLatch start = new CountDownLatch(1);

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = List.of(
                    executor.submit(statusOf(start, post("/customer/serviceOrders/{publicId}/approve", publicId)
                            .header("X-Customer-Document", customerDocument.value()))),
                    executor.submit(statusOf(start, authenticated(
                            post("/admin/serviceOrders/{id}/reopenQuote", serviceOrderId)))));
            start.countDown();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(statuses).filteredOn(code -> code < 300).hasSize(1);
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
        assertThat(finalStatus).isIn("QUOTE_APPROVED", "IN_DIAGNOSIS");
        assertThat(reservedOf(filter)).isZero();
        assertThat(stockOf(filter)).isEqualTo(finalStatus.equals("QUOTE_APPROVED") ? 2 : 5);
    }

    private Callable<Integer> statusOf(CountDownLatch start, MockHttpServletRequestBuilder request) {
        return () -> {
            start.await();
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        };
    }

    private ResultActions sendQuote() throws Exception {
        return as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId));
    }

    private Long addPart(Long partId, int quantity) throws Exception {
        return idOf(as(post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity)))
                .andExpect(status().isCreated()));
    }

    private Long addService() throws Exception {
        String body = as(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Troca de pastilhas", "laborPrice": 150, "estimatedMinutes": 60}
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long serviceOfferingId = ((Number) JsonPath.read(body, "$.id")).longValue();
        return idOf(as(post("/admin/serviceOrders/{id}/services", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOfferingId": %d, "quantity": 1}
                                """.formatted(serviceOfferingId)))
                .andExpect(status().isCreated()));
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "WD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long partId = idOf(as(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)))
                .andExpect(status().isCreated()));
        as(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private Long openServiceOrder() throws Exception {
        customerDocument = uniqueCPF();
        as(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(customerDocument.value())))
                .andExpect(status().isCreated());
        return idOf(as(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(customerDocument.value(), uniquePlate())))
                .andExpect(status().isCreated()));
    }

    private int reservedOf(Long partId) throws Exception {
        String body = as(get("/admin/parts/{id}", partId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.reservedQuantity");
    }

    private int stockOf(Long partId) {
        return jdbcTemplate.queryForObject("SELECT stock_quantity FROM part WHERE id = ?", Integer.class, partId);
    }

    private long movementCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE service_order_id = ?", Long.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT from_status, to_status, description FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private static Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions as(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(authenticated(request));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        String token = tokenIssuer.issue(userId, Set.of(Role.ADMIN)).value();
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
