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
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;
import java.util.Set;
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
class StartDiagnosisIntegrationTest {

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
        mockMvc.perform(startDiagnosis()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyAttendants() throws Exception {
        as(Role.ATTENDANT, startDiagnosis()).andExpect(status().isForbidden());

        assertThat(serviceOrder().get("status")).isEqualTo("RECEIVED");
    }

    @Test
    void shouldLetMechanicsStartTheDiagnosisAsTheResponsibleMechanic() throws Exception {
        as(Role.MECHANIC, startDiagnosis())
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
        as(Role.ADMIN, startDiagnosis()).andExpect(status().isOk());

        assertThat(serviceOrder().get("status")).isEqualTo("IN_DIAGNOSIS");
    }

    @Test
    void shouldRefuseToStartTheDiagnosisTwice() throws Exception {
        as(Role.MECHANIC, startDiagnosis()).andExpect(status().isOk());

        as(Role.MECHANIC, startDiagnosis())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
    }

    @Test
    void shouldReportAnUnknownServiceOrder() throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        String body = as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())))
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

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
