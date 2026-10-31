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
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
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
class SupplementaryQuoteIntegrationTest {

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

    private CPFCNPJ customerDocument;

    private Long serviceOrderId;

    private Long padId;

    private Long padItemId;

    private Long serviceItemId;

    @BeforeEach
    void startTheExecutionWithAnApprovedQuote() throws Exception {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Atendente",
                        new Email("atendente-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ATTENDANT),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
        padId = createPart(10);

        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        serviceItemId = addService(savedService());
        padItemId = addPart(padId, 2);
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId))
                .andExpect(status().isOk());
        as(Role.ATTENDANT, approve()).andExpect(status().isOk());
        as(Role.MECHANIC, startExecution()).andExpect(status().isOk());
    }

    @Test
    void shouldWithdrawOnlyTheNewPartsWhenTheSupplementIsApproved() throws Exception {
        Long filterId = createPart(10);
        addPart(filterId, 1);

        as(Role.MECHANIC, sendQuote(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_SUPPLEMENTARY_APPROVAL"))
                .andExpect(jsonPath("$.canProceedWithoutChanges").value(true));
        as(Role.ATTENDANT, approve())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPLEMENTARY_APPROVED"))
                .andExpect(jsonPath("$.parts[1].approvedAt").isNotEmpty());

        assertThat(stockOf(padId)).isEqualTo(8);
        assertThat(stockOf(filterId)).isEqualTo(9);
        assertThat(statusOf()).isEqualTo("SUPPLEMENTARY_APPROVED");

        as(Role.MECHANIC, startExecution())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(statusHistory())
                .endsWith(
                        "QUOTE_APPROVED",
                        "IN_PROGRESS",
                        "AWAITING_SUPPLEMENTARY_APPROVAL",
                        "SUPPLEMENTARY_APPROVED",
                        "IN_PROGRESS");
    }

    @Test
    void shouldDropTheNewItemsWhenOnlyTheOriginalQuoteIsApproved() throws Exception {
        Long filterId = createPart(10);
        addPart(filterId, 1);
        as(Role.MECHANIC, sendQuote(true)).andExpect(status().isOk());

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/approveOriginalOnly", serviceOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPLEMENTARY_APPROVED"))
                .andExpect(jsonPath("$.parts.length()").value(1));

        assertThat(stockOf(filterId)).isEqualTo(10);
        assertThat(stockOf(padId)).isEqualTo(8);
        assertThat(lastStatusChange()).containsEntry("description", "Aprovado apenas o orçamento original.");
    }

    @Test
    void shouldReturnTheApprovedPartThatWasReplaced() throws Exception {
        Long filterId = createPart(10);
        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, padItemId))
                .andExpect(status().isNoContent());
        addPart(filterId, 1);

        as(Role.MECHANIC, sendQuote(true))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.originalOnlyNotPossible"));
        as(Role.MECHANIC, sendQuote(false)).andExpect(status().isOk());
        as(Role.ATTENDANT, approve()).andExpect(status().isOk());

        assertThat(stockOf(padId)).isEqualTo(10);
        assertThat(stockOf(filterId)).isEqualTo(9);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT", "IN");
    }

    @Test
    void shouldResendTheSameQuoteAfterARejectionWithoutMovingTheStock() throws Exception {
        Long filterId = createPart(10);
        Long filterItemId = addPart(filterId, 1);
        as(Role.MECHANIC, sendQuote(true)).andExpect(status().isOk());
        as(Role.ATTENDANT, reject())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPLEMENTARY_REJECTED"));
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/complete", serviceOrderId))
                .andExpect(status().isUnprocessableContent());

        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, filterItemId))
                .andExpect(status().isNoContent());
        as(Role.MECHANIC, sendQuote(true)).andExpect(status().isOk());
        as(Role.ATTENDANT, approve()).andExpect(status().isOk());

        assertThat(stockOf(padId)).isEqualTo(8);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT");
        assertThat(movementsOf(filterId)).containsExactly("IN");
    }

    @Test
    void shouldReturnEveryPartWhenTheServiceOrderIsCanceledAfterARejection() throws Exception {
        addPart(createPart(10), 1);
        as(Role.MECHANIC, sendQuote(true)).andExpect(status().isOk());
        as(Role.ATTENDANT, reject()).andExpect(status().isOk());

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/cancel", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Cliente desistiu do reparo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(stockOf(padId)).isEqualTo(10);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT", "IN");
    }

    @Test
    void shouldNotCompleteAServiceAddedDuringTheExecutionBeforeTheApproval() throws Exception {
        Long newServiceItemId = addService(savedService());

        as(Role.MECHANIC, post(
                        "/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, newServiceItemId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingItemCompletionNotAllowed"));
        as(Role.MECHANIC, post(
                        "/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, serviceItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/complete", serviceOrderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingServiceItems"));
    }

    @Test
    void shouldLetTheCustomerChooseTheSupplementaryQuoteThroughTheirApi() throws Exception {
        Long filterId = createPart(10);
        addPart(filterId, 1);
        as(Role.MECHANIC, sendQuote(true)).andExpect(status().isOk());

        mockMvc.perform(get("/customer/serviceOrders/{publicId}", publicId())
                        .header("X-Customer-Document", customerDocument.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_SUPPLEMENTARY_APPROVAL"))
                .andExpect(jsonPath("$.quote.services[0].stage").value("APPROVED"))
                .andExpect(jsonPath("$.quote.parts[0].stage").value("APPROVED"))
                .andExpect(jsonPath("$.quote.parts[1].stage").value("PENDING"))
                .andExpect(jsonPath("$.quote.canProceedWithoutChanges").value(true));

        mockMvc.perform(post("/customer/serviceOrders/{publicId}/approveOriginalOnly", publicId())
                        .header("X-Customer-Document", customerDocument.value()))
                .andExpect(status().isNoContent());

        assertThat(statusOf()).isEqualTo("SUPPLEMENTARY_APPROVED");
        assertThat(lastStatusChange()).containsEntry("changed_by_user_id", null);
        assertThat(stockOf(filterId)).isEqualTo(10);
    }

    @Test
    void shouldReturnAReplacedPartWithoutUserWhenTheCustomerApproves() throws Exception {
        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, padItemId))
                .andExpect(status().isNoContent());
        as(Role.MECHANIC, sendQuote(false)).andExpect(status().isOk());

        mockMvc.perform(post("/customer/serviceOrders/{publicId}/approve", publicId())
                        .header("X-Customer-Document", customerDocument.value()))
                .andExpect(status().isNoContent());

        assertThat(stockOf(padId)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                        """
                        SELECT created_by_user_id IS NULL FROM stock_movement
                        WHERE part_id = ? AND type = 'IN' AND service_order_id = ?
                        """,
                        Boolean.class,
                        padId,
                        serviceOrderId))
                .isTrue();
    }

    private MockHttpServletRequestBuilder sendQuote(boolean canProceedWithoutChanges) {
        return post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"canProceedWithoutChanges": %s}
                        """.formatted(canProceedWithoutChanges));
    }

    private MockHttpServletRequestBuilder approve() {
        return post("/admin/serviceOrders/{id}/approve", serviceOrderId);
    }

    private MockHttpServletRequestBuilder reject() {
        return post("/admin/serviceOrders/{id}/reject", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason": "Achou caro"}
                        """);
    }

    private MockHttpServletRequestBuilder startExecution() {
        return post("/admin/serviceOrders/{id}/startExecution", serviceOrderId);
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
        String sku = "SQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
        customerDocument = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(customerDocument.value())))
                .andExpect(status().isCreated());
        return idOf(as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(customerDocument.value(), uniquePlate())))
                .andExpect(status().isCreated()));
    }

    private static Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private UUID publicId() {
        return jdbcTemplate.queryForObject(
                "SELECT public_id FROM service_order WHERE id = ?", UUID.class, serviceOrderId);
    }

    private int stockOf(Long partId) throws Exception {
        String body = as(Role.ADMIN, get("/admin/parts/{id}", partId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.stockQuantity");
    }

    private List<String> movementsOf(Long partId) {
        return jdbcTemplate.queryForList(
                "SELECT type FROM stock_movement WHERE part_id = ? ORDER BY id", String.class, partId);
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private List<String> statusHistory() {
        return jdbcTemplate.queryForList(
                "SELECT to_status FROM status_change WHERE service_order_id = ? ORDER BY id",
                String.class,
                serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT to_status, changed_by_user_id, description FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
