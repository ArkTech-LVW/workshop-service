package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import static br.com.arktech.workshop.TestUsers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.TestUsers;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StockAdjustmentRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long attendantId;
    private Long adminId;
    private Long otherAdminId;

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(post("/admin/parts/1/stockAdjustmentRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")),
                Arguments.of(get("/admin/stockAdjustmentRequests")),
                Arguments.of(post("/admin/stockAdjustmentRequests/1/approve")),
                Arguments.of(post("/admin/stockAdjustmentRequests/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")));
    }

    static Stream<Arguments> adminOnlyEndpoints() {
        return Stream.of(
                Arguments.of(get("/admin/stockAdjustmentRequests")),
                Arguments.of(post("/admin/stockAdjustmentRequests/1/approve")),
                Arguments.of(post("/admin/stockAdjustmentRequests/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewNote": "Engano"}
                                """)));
    }

    @BeforeEach
    void setUp() {
        attendantId = testUsers.save(Role.ATTENDANT).getId();
        adminId = testUsers.save(Role.ADMIN).getId();
        otherAdminId = testUsers.save(Role.ADMIN).getId();
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("adminOnlyEndpoints")
    void shouldDenyAttendantOnQueueAndReview(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(attendantId, Role.ATTENDANT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void shouldDenyMechanic(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(attendantId, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldDecreaseStockOnlyAfterApprovalAndShowWhoRequestedAndApproved() throws Exception {
        Integer partId = createPartWithStock(5);

        Integer requestId = openRequest(partId, 2, "Avaria no transporte", attendantId, Role.ATTENDANT);
        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(5));
        assertThat(pendingRequestIds()).contains(requestId);

        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", requestId)
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.requestedByUserId").value(attendantId))
                .andExpect(jsonPath("$.reviewedByUserId").value(adminId))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());

        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(3));
        assertThat(pendingRequestIds()).doesNotContain(requestId);
        mockMvc.perform(get("/admin/parts/{id}/stockMovements", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].reason").value("Avaria no transporte"))
                .andExpect(jsonPath("$.content[0].stockAdjustmentRequestId").value(requestId))
                .andExpect(jsonPath("$.content[0].requestedByUserId").value(attendantId))
                .andExpect(jsonPath("$.content[0].createdByUserId").value(adminId));
    }

    @Test
    void shouldNotLetAdminApproveOwnRequest() throws Exception {
        Integer partId = createPartWithStock(5);
        Integer requestId = openRequest(partId, 1, "Perda", adminId, Role.ADMIN);

        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", requestId)
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("stockAdjustmentRequest.selfReview"));
        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(5));

        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", requestId)
                        .with(bearer(otherAdminId, Role.ADMIN)))
                .andExpect(status().isOk());
        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(4));
    }

    @Test
    void shouldRejectWithNoteAndNeverReviewAgain() throws Exception {
        Integer partId = createPartWithStock(5);
        Integer requestId = openRequest(partId, 2, "Avaria", attendantId, Role.ATTENDANT);

        mockMvc.perform(reject(requestId, " ").with(bearer(adminId, Role.ADMIN))).andExpect(status().isBadRequest());
        mockMvc.perform(reject(requestId, "Peça encontrada no almoxarifado").with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNote").value("Peça encontrada no almoxarifado"));

        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", requestId)
                        .with(bearer(otherAdminId, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("stockAdjustmentRequest.alreadyReviewed"));
        mockMvc.perform(reject(requestId, "Outra observação").with(bearer(otherAdminId, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("stockAdjustmentRequest.alreadyReviewed"));
        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(5));
        assertThat(adjustmentCount(partId)).isZero();
    }

    @Test
    void shouldCheckStockAtApprovalTime() throws Exception {
        Integer partId = createPartWithStock(3);
        Integer requestId = openRequest(partId, 3, "Avaria", attendantId, Role.ATTENDANT);
        directAdjustment(partId, 2).andExpect(status().isCreated());

        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", requestId)
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        stockOf(partId).andExpect(jsonPath("$.stockQuantity").value(1));
        assertThat(pendingRequestIds()).contains(requestId);
    }

    @Test
    void shouldReportUnknownRequest() throws Exception {
        mockMvc.perform(post("/admin/stockAdjustmentRequests/{id}/approve", Long.MAX_VALUE)
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("stockAdjustmentRequest.notFound"));
    }

    private Integer openRequest(Integer partId, int quantity, String reason, Long userId, Role role)
            throws Exception {
        String body = mockMvc.perform(openRequestCall(partId, quantity, reason).with(bearer(userId, role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedByUserId").value(userId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static MockHttpServletRequestBuilder openRequestCall(Integer partId, int quantity, String reason) {
        return post("/admin/parts/{id}/stockAdjustmentRequests", partId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quantity": %d, "reason": "%s"}
                        """.formatted(quantity, reason));
    }

    private static MockHttpServletRequestBuilder reject(Integer requestId, String reviewNote) {
        return post("/admin/stockAdjustmentRequests/{id}/reject", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reviewNote": "%s"}
                        """.formatted(reviewNote));
    }

    private List<Integer> pendingRequestIds() throws Exception {
        String body = mockMvc.perform(get("/admin/stockAdjustmentRequests")
                        .param("status", "PENDING")
                        .param("size", "100")
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.content[*].id");
    }

    private ResultActions directAdjustment(Integer partId, int quantity) throws Exception {
        return mockMvc.perform(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ADJUSTMENT", "quantity": %d, "reason": "Contagem"}
                                """.formatted(quantity))
                        .with(bearer(adminId, Role.ADMIN)));
    }

    private ResultActions stockOf(Integer partId) throws Exception {
        return mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    private Integer adjustmentCount(Integer partId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE part_id = ? AND type = 'ADJUSTMENT'",
                Integer.class,
                partId);
    }

    private Integer createPartWithStock(int stock) throws Exception {
        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String created = mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT",
                         "salePrice": 32, "minimumStock": 0}
                        """.formatted(sku)).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer partId = JsonPath.read(created, "$.id");
        mockMvc.perform(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stock))
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isCreated());
        return partId;
    }
}
