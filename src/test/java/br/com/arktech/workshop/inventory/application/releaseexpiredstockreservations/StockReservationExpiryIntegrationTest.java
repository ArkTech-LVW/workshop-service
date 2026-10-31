package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
class StockReservationExpiryIntegrationTest {

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

    @Autowired
    private ExpiredStockReservationsJob expiredStockReservationsJob;

    @Autowired
    private ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase;

    private Long userId;

    @BeforeEach
    void createTheAttendant() {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Atendente",
                        new Email("atendente-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ADMIN),
                        passwordHasher))
                .getId();
    }

    @Test
    void shouldReleaseOnlyTheExpiredReservationsAndKeepTheServiceOrderWaiting() throws Exception {
        Long filter = createPart(10);
        ServiceOrder expired = sentServiceOrderWith(filter, 3);
        ServiceOrder valid = sentServiceOrderWith(filter, 2);
        expireTheReservationOf(expired);

        expiredStockReservationsJob.releaseExpiredReservations();

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(2));
        assertThat(reservationCount(expired)).isZero();
        assertThat(reservationCount(valid)).isOne();
        assertThat(statusOf(expired)).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldApproveAfterTheExpirationWhenTheStockStillCoversIt() throws Exception {
        Long filter = createPart(5);
        ServiceOrder serviceOrder = sentServiceOrderWith(filter, 3);
        expireTheReservationOf(serviceOrder);
        expiredStockReservationsJob.releaseExpiredReservations();

        approveAsTheCustomer(serviceOrder).andExpect(status().isNoContent());

        assertThat(statusOf(serviceOrder)).isEqualTo("QUOTE_APPROVED");
        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(2))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void shouldRefuseTheApprovalWhenTheStockIsGoneAfterTheExpiration() throws Exception {
        Long filter = createPart(5);
        ServiceOrder serviceOrder = sentServiceOrderWith(filter, 3);
        expireTheReservationOf(serviceOrder);
        expiredStockReservationsJob.releaseExpiredReservations();
        sentServiceOrderWith(filter, 3);

        approveAsTheCustomer(serviceOrder)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.partsUnavailable"))
                .andExpect(jsonPath("$.message")
                        .value("As peças do orçamento não estão mais disponíveis. Fale com a oficina."));
        as(post("/admin/serviceOrders/{id}/approve", serviceOrder.id()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        assertThat(statusOf(serviceOrder)).isEqualTo("AWAITING_APPROVAL");
        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(3));
    }

    @Test
    void shouldReleaseEachExpiredReservationOnceWhenTwoJobsRunAtTheSameTime() throws Exception {
        Long filter = createPart(5);
        ServiceOrder serviceOrder = sentServiceOrderWith(filter, 3);
        expireTheReservationOf(serviceOrder);
        ReleaseExpiredStockReservationsCommand command = new ReleaseExpiredStockReservationsCommand(Instant.now());
        CountDownLatch start = new CountDownLatch(1);

        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<?>> results = new ArrayList<>();
            for (int job = 0; job < 2; job++) {
                results.add(executor.submit(() -> {
                    start.await();
                    releaseExpiredStockReservationsUseCase.execute(command);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException ex) {
                    failures.add(ex.getCause());
                }
            }
        }

        assertThat(failures).isEmpty();
        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservationCount(serviceOrder)).isZero();
    }

    private ServiceOrder sentServiceOrderWith(Long partId, int quantity) throws Exception {
        ServiceOrder serviceOrder = openServiceOrder();
        as(post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrder.id())).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/parts", serviceOrder.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity)))
                .andExpect(status().isCreated());
        as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrder.id())).andExpect(status().isOk());
        return serviceOrder;
    }

    private void expireTheReservationOf(ServiceOrder serviceOrder) {
        jdbcTemplate.update(
                "UPDATE stock_reservation SET expires_at = now() - interval '1 minute' WHERE service_order_id = ?",
                serviceOrder.id());
    }

    private ResultActions approveAsTheCustomer(ServiceOrder serviceOrder) throws Exception {
        UUID publicId = jdbcTemplate.queryForObject(
                "SELECT public_id FROM service_order WHERE id = ?", UUID.class, serviceOrder.id());
        return mockMvc.perform(post("/customer/serviceOrders/{publicId}/approve", publicId)
                .header("X-Customer-Document", serviceOrder.customerDocument().value()));
    }

    private ResultActions part(Long partId) throws Exception {
        return as(get("/admin/parts/{id}", partId)).andExpect(status().isOk());
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "EX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = as(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long partId = ((Number) JsonPath.read(body, "$.id")).longValue();
        as(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private ServiceOrder openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        String body = as(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new ServiceOrder(((Number) JsonPath.read(body, "$.id")).longValue(), document);
    }

    private long reservationCount(ServiceOrder serviceOrder) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_reservation WHERE service_order_id = ?", Long.class, serviceOrder.id());
    }

    private String statusOf(ServiceOrder serviceOrder) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrder.id());
    }

    private ResultActions as(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(Role.ADMIN)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private record ServiceOrder(Long id, CPFCNPJ customerDocument) {
    }
}
