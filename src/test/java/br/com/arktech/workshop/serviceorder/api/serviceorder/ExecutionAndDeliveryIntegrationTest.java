package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
class ExecutionAndDeliveryIntegrationTest {

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
    private ServiceOfferingRepository serviceOfferingRepository;

    private Long userId;

    private Long serviceOrderId;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Mecânico",
                        new Email("mecanico-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.MECHANIC),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(startExecution()).andExpect(status().isUnauthorized());
        mockMvc.perform(completeService(1L)).andExpect(status().isUnauthorized());
        mockMvc.perform(complete()).andExpect(status().isUnauthorized());
        mockMvc.perform(deliver()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunTheExecutionServiceByServiceAndDeliverRecordingTheDates() throws Exception {
        List<Long> itemIds = approveQuoteWithServices(2);

        as(Role.ATTENDANT, startExecution()).andExpect(status().isForbidden());
        as(Role.MECHANIC, startExecution())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(lastStatusChange())
                .containsEntry("from_status", "QUOTE_APPROVED")
                .containsEntry("to_status", "IN_PROGRESS")
                .containsEntry("changed_by_user_id", userId);

        as(Role.MECHANIC, complete())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingServiceItems"));
        as(Role.ATTENDANT, startService(itemIds.getFirst())).andExpect(status().isForbidden());
        as(Role.ATTENDANT, completeService(itemIds.getFirst())).andExpect(status().isForbidden());
        as(Role.MECHANIC, completeService(itemIds.getFirst()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.serviceItemNotStarted"));

        as(Role.MECHANIC, startService(itemIds.getFirst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.services[0].startedAt").isNotEmpty())
                .andExpect(jsonPath("$.services[1].startedAt").isEmpty());
        as(Role.MECHANIC, startService(itemIds.getFirst()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.serviceItemAlreadyStarted"));
        as(Role.ADMIN, startService(itemIds.getLast())).andExpect(status().isOk());

        as(Role.MECHANIC, completeService(itemIds.getFirst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.services[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.services[1].completedAt").isEmpty());
        as(Role.MECHANIC, completeService(itemIds.getFirst()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.serviceItemAlreadyCompleted"));

        as(Role.ADMIN, completeService(itemIds.getLast()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(lastStatusChange())
                .containsEntry("from_status", "IN_PROGRESS")
                .containsEntry("to_status", "COMPLETED")
                .containsEntry("changed_by_user_id", userId);
        assertThat(jdbcTemplate.queryForList(
                        "SELECT completed_by_user_id FROM service_item WHERE service_order_id = ?",
                        Long.class,
                        serviceOrderId))
                .containsOnly(userId);
        OffsetDateTime executionStartedAt = statusChangeDate("IN_PROGRESS");
        OffsetDateTime executionFinishedAt = statusChangeDate("COMPLETED");
        assertThat(executionFinishedAt).isAfterOrEqualTo(executionStartedAt);
        assertThat(jdbcTemplate.queryForList(
                        "SELECT completed_at FROM service_item WHERE service_order_id = ?",
                        OffsetDateTime.class,
                        serviceOrderId))
                .hasSize(2)
                .allSatisfy(completedAt -> assertThat(completedAt).isAfterOrEqualTo(executionStartedAt));
        assertThat(jdbcTemplate.queryForList(
                        "SELECT started_by_user_id FROM service_item WHERE service_order_id = ?",
                        Long.class,
                        serviceOrderId))
                .containsOnly(userId);

        as(Role.MECHANIC, deliver()).andExpect(status().isForbidden());
        as(Role.ATTENDANT, deliver())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        assertThat(lastStatusChange())
                .containsEntry("from_status", "COMPLETED")
                .containsEntry("to_status", "DELIVERED");
    }

    @Test
    void shouldCompleteAServiceOrderWithPartsOnlyManually() throws Exception {
        sendQuote(() -> addPart(createPart(5), 2));
        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());
        as(Role.MECHANIC, startExecution()).andExpect(status().isOk());

        as(Role.MECHANIC, complete())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(lastStatusChange())
                .containsEntry("from_status", "IN_PROGRESS")
                .containsEntry("to_status", "COMPLETED")
                .containsEntry("changed_by_user_id", userId);
    }

    @Test
    void shouldRefuseToStartTheExecutionOfAQuoteThatWasNotApproved() throws Exception {
        sendQuote(() -> addService(savedService()));

        as(Role.MECHANIC, startExecution())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(statusOf()).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldRefuseToCompleteAServiceBeforeTheExecutionStarts() throws Exception {
        List<Long> itemIds = approveQuoteWithServices(1);

        as(Role.MECHANIC, completeService(itemIds.getFirst()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.serviceItemCompletionNotAllowed"));

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
    }

    @Test
    void shouldRefuseToStartAServiceBeforeTheExecutionStarts() throws Exception {
        List<Long> itemIds = approveQuoteWithServices(1);

        as(Role.MECHANIC, startService(itemIds.getFirst()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.serviceItemStartNotAllowed"));

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
    }

    @Test
    void shouldReportAServiceThatIsNotInTheServiceOrder() throws Exception {
        approveQuoteWithServices(1);
        as(Role.MECHANIC, startExecution()).andExpect(status().isOk());

        as(Role.MECHANIC, completeService(Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemNotFound"));
    }

    @Test
    void shouldRefuseToDeliverAServiceOrderThatIsNotCompleted() throws Exception {
        approveQuoteWithServices(1);
        as(Role.MECHANIC, startExecution()).andExpect(status().isOk());

        as(Role.ATTENDANT, deliver())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(statusOf()).isEqualTo("IN_PROGRESS");
    }

    private List<Long> approveQuoteWithServices(int count) throws Exception {
        List<Long> itemIds = new ArrayList<>();
        sendQuote(() -> {
            for (int i = 0; i < count; i++) {
                itemIds.add(addService(savedService()));
            }
        });
        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());
        return itemIds;
    }

    private void sendQuote(ItemsStep addItems) throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        addItems.run();
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId))
                .andExpect(status().isOk());
    }

    private Long addService(Long serviceOfferingId) throws Exception {
        return idOf(as(Role.MECHANIC, post("/admin/serviceOrders/{id}/services", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOfferingId": %d, "quantity": 1}
                                """.formatted(serviceOfferingId)))
                .andExpect(status().isCreated()));
    }

    private Long addPart(Long partId, int quantity) throws Exception {
        return idOf(as(Role.MECHANIC, post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity)))
                .andExpect(status().isCreated()));
    }

    private Long savedService() {
        return serviceOfferingRepository
                .save(new ServiceOffering("Freios", null, new Money(new BigDecimal("150.00")), 60))
                .getId();
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "EX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long partId = idOf(as(Role.ADMIN, post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)))
                .andExpect(status().isCreated()));
        as(Role.ADMIN, post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        return idOf(as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())))
                .andExpect(status().isCreated()));
    }

    private static Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT from_status, to_status, changed_by_user_id FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private OffsetDateTime statusChangeDate(String toStatus) {
        return jdbcTemplate.queryForObject(
                "SELECT date_created FROM status_change WHERE service_order_id = ? AND to_status = ?",
                OffsetDateTime.class,
                serviceOrderId,
                toStatus);
    }

    private MockHttpServletRequestBuilder startExecution() {
        return post("/admin/serviceOrders/{id}/startExecution", serviceOrderId);
    }

    private MockHttpServletRequestBuilder startService(Long itemId) {
        return post("/admin/serviceOrders/{id}/services/{itemId}/start", serviceOrderId, itemId);
    }

    private MockHttpServletRequestBuilder completeService(Long itemId) {
        return post("/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, itemId);
    }

    private MockHttpServletRequestBuilder complete() {
        return post("/admin/serviceOrders/{id}/complete", serviceOrderId);
    }

    private MockHttpServletRequestBuilder deliver() {
        return post("/admin/serviceOrders/{id}/deliver", serviceOrderId);
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @FunctionalInterface
    private interface ItemsStep {
        void run() throws Exception;
    }
}
