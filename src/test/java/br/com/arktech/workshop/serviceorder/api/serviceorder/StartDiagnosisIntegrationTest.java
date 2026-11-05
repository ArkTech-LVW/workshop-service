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
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StartDiagnosisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    private Long serviceOrderId;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        userId = testUsers.save(Role.MECHANIC).getId();
        serviceOrderId = openServiceOrder();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(startDiagnosis()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyAttendants() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isForbidden());

        assertThat(serviceOrder().get("status")).isEqualTo("RECEIVED");
    }

    @Test
    void shouldLetMechanicsStartTheDiagnosisAsTheResponsibleMechanic() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))
                .andExpect(jsonPath("$.mechanicId").value(userId))
                .andExpect(jsonPath("$.services").isEmpty())
                .andExpect(jsonPath("$.parts").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0.0));

        Map<String, Object> serviceOrder = serviceOrder();
        assertThat(serviceOrder.get("status")).isEqualTo("IN_DIAGNOSIS");
        assertThat(serviceOrder.get("mechanic_id")).isEqualTo(userId);
        assertThat(jdbcTemplate.queryForMap(
                        """
                        SELECT from_status, to_status, changed_by_user_id FROM status_change
                        WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                        """,
                        serviceOrderId))
                .containsEntry("from_status", "RECEIVED")
                .containsEntry("to_status", "IN_DIAGNOSIS")
                .containsEntry("changed_by_user_id", userId);
    }

    @Test
    void shouldLetAdminsStartTheDiagnosis() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.ADMIN))).andExpect(status().isOk());

        assertThat(serviceOrder().get("status")).isEqualTo("IN_DIAGNOSIS");
    }

    @Test
    void shouldRefuseToStartTheDiagnosisTwice() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
    }

    @Test
    void shouldReportAnUnknownServiceOrder() throws Exception {
        mockMvc.perform(post("/admin/serviceOrders/{id}/startDiagnosis", Long.MAX_VALUE)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private Map<String, Object> serviceOrder() {
        return jdbcTemplate.queryForMap("SELECT status, mechanic_id FROM service_order WHERE id = ?", serviceOrderId);
    }

    private MockHttpServletRequestBuilder startDiagnosis() {
        return post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId);
    }
}
