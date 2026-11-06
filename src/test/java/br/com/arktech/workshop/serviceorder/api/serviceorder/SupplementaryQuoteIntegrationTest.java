package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.TestUsers;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        userId = testUsers.save(Role.ATTENDANT).getId();
        serviceOrderId = openServiceOrder();
        padId = createPart(10);

        mockMvc.perform(post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk());
        serviceItemId = addService(savedService());
        padItemId = addPart(padId, 2);
        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk());
        mockMvc.perform(approve().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());
        mockMvc.perform(startExecution().with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
    }

    @Test
    void shouldNotCountTheApprovedPartsAgainstTheAvailableStockWhenAddingDuringTheExecution() throws Exception {
        addPart(padId, 8);

        mockMvc.perform(post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": 1}
                                """.formatted(padId))
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));
        assertThat(stockOf(padId)).isEqualTo(8);
    }

    @Test
    void shouldWithdrawOnlyTheNewPartsWhenTheSupplementIsApproved() throws Exception {
        Long filterId = createPart(10);
        addPart(filterId, 1);

        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_SUPPLEMENTARY_APPROVAL"))
                .andExpect(jsonPath("$.canProceedWithoutChanges").value(true));
        mockMvc.perform(approve().with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPLEMENTARY_APPROVED"))
                .andExpect(jsonPath("$.parts[1].approvedAt").isNotEmpty());

        assertThat(stockOf(padId)).isEqualTo(8);
        assertThat(stockOf(filterId)).isEqualTo(9);
        assertThat(statusOf()).isEqualTo("SUPPLEMENTARY_APPROVED");

        mockMvc.perform(startExecution().with(bearer(userId, Role.MECHANIC)))
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
        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

        mockMvc.perform(post("/admin/serviceOrders/{id}/approveOriginalOnly", serviceOrderId)
                        .with(bearer(userId, Role.ATTENDANT)))
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
        mockMvc.perform(delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, padItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isNoContent());
        addPart(filterId, 1);

        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.originalOnlyNotPossible"));
        mockMvc.perform(sendQuote(false).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
        mockMvc.perform(approve().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());

        assertThat(stockOf(padId)).isEqualTo(10);
        assertThat(stockOf(filterId)).isEqualTo(9);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT", "IN");
    }

    @Test
    void shouldResendTheSameQuoteAfterARejectionWithoutMovingTheStock() throws Exception {
        Long filterId = createPart(10);
        Long filterItemId = addPart(filterId, 1);
        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
        mockMvc.perform(reject().with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPLEMENTARY_REJECTED"));
        mockMvc.perform(post("/admin/serviceOrders/{id}/complete", serviceOrderId).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, filterItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isNoContent());
        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
        mockMvc.perform(approve().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());

        assertThat(stockOf(padId)).isEqualTo(8);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT");
        assertThat(movementsOf(filterId)).containsExactly("IN");
    }

    @Test
    void shouldReturnEveryPartWhenTheServiceOrderIsCanceledAfterARejection() throws Exception {
        addPart(createPart(10), 1);
        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
        mockMvc.perform(reject().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());

        mockMvc.perform(post("/admin/serviceOrders/{id}/cancel", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Cliente desistiu do reparo"}
                                """)
                        .with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(stockOf(padId)).isEqualTo(10);
        assertThat(movementsOf(padId)).containsExactly("IN", "OUT", "IN");
    }

    @Test
    void shouldNotCompleteAServiceAddedDuringTheExecutionBeforeTheApproval() throws Exception {
        Long newServiceItemId = addService(savedService());

        mockMvc.perform(post(
                        "/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, newServiceItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingItemCompletionNotAllowed"));
        mockMvc.perform(post(
                        "/admin/serviceOrders/{id}/services/{itemId}/start", serviceOrderId, newServiceItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingItemStartNotAllowed"));
        mockMvc.perform(post(
                        "/admin/serviceOrders/{id}/services/{itemId}/start", serviceOrderId, serviceItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/admin/serviceOrders/{id}/services/{itemId}/complete", serviceOrderId, serviceItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(post("/admin/serviceOrders/{id}/complete", serviceOrderId).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.pendingServiceItems"));
    }

    @Test
    void shouldLetTheCustomerChooseTheSupplementaryQuoteThroughTheirApi() throws Exception {
        Long filterId = createPart(10);
        addPart(filterId, 1);
        mockMvc.perform(sendQuote(true).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

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
        mockMvc.perform(delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, padItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isNoContent());
        mockMvc.perform(sendQuote(false).with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

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
        return idOf(mockMvc.perform(post("/admin/serviceOrders/{id}/services", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOfferingId": %d, "quantity": 1}
                                """.formatted(serviceOfferingId))
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isCreated()));
    }

    private Long addPart(Long partId, int quantity) throws Exception {
        return idOf(mockMvc.perform(post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity))
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isCreated()));
    }

    private Long savedService() {
        return serviceOfferingRepository
                .save(new ServiceOffering("Freios", null, new Money(new BigDecimal("150.00")), 60))
                .getId();
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "SQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long partId = idOf(mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated()));
        mockMvc.perform(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity))
                        .with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated());
        return partId;
    }

    private Long openServiceOrder() throws Exception {
        customerDocument = uniqueCPF();
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(customerDocument.value())).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated());
        return idOf(mockMvc.perform(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(customerDocument.value(), uniquePlate())).with(bearer(userId, Role.ATTENDANT)))
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
        String body = mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(userId, Role.ADMIN)))
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
}
