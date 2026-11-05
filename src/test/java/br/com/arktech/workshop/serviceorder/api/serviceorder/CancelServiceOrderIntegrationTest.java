package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.TestUsers;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class CancelServiceOrderIntegrationTest {

    private static final String REASON = "Cliente desistiu do serviço";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void saveTheUserBehindTheTokens() {
        userId = testUsers.save(Role.ATTENDANT).getId();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(cancelRequest(1L, REASON)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyMechanics() throws Exception {
        Long serviceOrderId = openServiceOrder();

        mockMvc.perform(cancelRequest(serviceOrderId, REASON)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));

        assertThat(statusOf(serviceOrderId)).isEqualTo("RECEIVED");
    }

    @ParameterizedTest
    @EnumSource(
            value = ServiceOrderStatus.class,
            names = {"RECEIVED", "IN_DIAGNOSIS", "AWAITING_APPROVAL", "QUOTE_REJECTED"})
    void shouldCancelRecordingTheReasonAndWhoCanceled(ServiceOrderStatus current) throws Exception {
        Long serviceOrderId = openServiceOrderIn(current);

        mockMvc.perform(cancelRequest(serviceOrderId, REASON).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceOrderId))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(statusOf(serviceOrderId)).isEqualTo("CANCELED");
        assertThat(lastStatusChangeOf(serviceOrderId))
                .containsEntry("from_status", current.name())
                .containsEntry("to_status", "CANCELED")
                .containsEntry("changed_by_user_id", userId)
                .containsEntry("description", REASON);
    }

    @ParameterizedTest
    @EnumSource(
            value = ServiceOrderStatus.class,
            names = {"QUOTE_APPROVED", "IN_PROGRESS", "COMPLETED", "DELIVERED", "CANCELED"})
    void shouldRefuseToCancelAfterTheQuoteIsApprovedOrTwice(ServiceOrderStatus current) throws Exception {
        Long serviceOrderId = openServiceOrderIn(current);

        mockMvc.perform(cancelRequest(serviceOrderId, REASON).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(statusOf(serviceOrderId)).isEqualTo(current.name());
        assertThat(statusChangeCountOf(serviceOrderId)).isOne();
    }

    @Test
    void shouldRequireAReasonWithoutChangingTheServiceOrder() throws Exception {
        Long serviceOrderId = openServiceOrder();

        mockMvc.perform(cancelRequest(serviceOrderId, " ").with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("reason"));

        assertThat(statusOf(serviceOrderId)).isEqualTo("RECEIVED");
        assertThat(statusChangeCountOf(serviceOrderId)).isOne();
    }

    @Test
    void shouldAnswer404ForAnUnknownServiceOrder() throws Exception {
        mockMvc.perform(cancelRequest(Long.MAX_VALUE, REASON).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private Long openServiceOrderIn(ServiceOrderStatus current) throws Exception {
        Long serviceOrderId = openServiceOrder();
        jdbcTemplate.update("UPDATE service_order SET status = ? WHERE id = ?", current.name(), serviceOrderId);
        return serviceOrderId;
    }

    private Long openServiceOrder() throws Exception {
        String document = uniqueCPF().value();
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document)).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document, uniquePlate())).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String statusOf(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChangeOf(Long serviceOrderId) {
        return jdbcTemplate.queryForMap(
                """
                SELECT from_status, to_status, changed_by_user_id, description
                FROM status_change WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private long statusChangeCountOf(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM status_change WHERE service_order_id = ?", Long.class, serviceOrderId);
    }

    private static MockHttpServletRequestBuilder cancelRequest(Long serviceOrderId, String reason) {
        return post("/admin/serviceOrders/{id}/cancel", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason": "%s"}
                        """.formatted(reason));
    }
}
