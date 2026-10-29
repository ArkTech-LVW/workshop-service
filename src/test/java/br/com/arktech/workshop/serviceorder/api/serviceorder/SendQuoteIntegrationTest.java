package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class SendQuoteIntegrationTest {

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
                        "Atendente",
                        new Email("atendente-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ATTENDANT),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(sendQuote()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSendTheQuoteRecordingWhoSentIt() throws Exception {
        diagnoseWithAService();

        as(Role.ATTENDANT, sendQuote())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$.services.length()").value(1))
                .andExpect(jsonPath("$.totalPrice").value(300.0));

        assertThat(statusOf()).isEqualTo("AWAITING_APPROVAL");
        assertThat(lastStatusChange())
                .containsEntry("from_status", "IN_DIAGNOSIS")
                .containsEntry("to_status", "AWAITING_APPROVAL")
                .containsEntry("changed_by_user_id", userId);
        assertThat(lastStatusChange().get("date_created")).isNotNull();
    }

    @Test
    void shouldLetAdminsAndMechanicsSendTheQuote() throws Exception {
        diagnoseWithAService();
        as(Role.MECHANIC, sendQuote()).andExpect(status().isOk());

        serviceOrderId = openServiceOrder();
        diagnoseWithAService();
        as(Role.ADMIN, sendQuote()).andExpect(status().isOk());
    }

    @Test
    void shouldLockTheItemsAfterSending() throws Exception {
        Long serviceItemId = diagnoseWithAService();
        as(Role.ATTENDANT, sendQuote()).andExpect(status().isOk());

        as(Role.MECHANIC, addService(savedService()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));
        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/services/{itemId}", serviceOrderId, serviceItemId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM service_item WHERE service_order_id = ?", Long.class, serviceOrderId))
                .isOne();
    }

    @Test
    void shouldRefuseAQuoteWithoutItemsWithoutChangingTheServiceOrder() throws Exception {
        as(Role.MECHANIC, startDiagnosis()).andExpect(status().isOk());

        as(Role.ATTENDANT, sendQuote())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.quoteWithoutItems"));

        assertThat(statusOf()).isEqualTo("IN_DIAGNOSIS");
        assertThat(lastStatusChange()).containsEntry("to_status", "IN_DIAGNOSIS");
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotInDiagnosis() throws Exception {
        as(Role.ATTENDANT, sendQuote())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(statusOf()).isEqualTo("RECEIVED");
    }

    @Test
    void shouldRefuseToSendTheQuoteTwice() throws Exception {
        diagnoseWithAService();
        as(Role.ATTENDANT, sendQuote()).andExpect(status().isOk());

        as(Role.ATTENDANT, sendQuote())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(jdbcTemplate.queryForObject(
                        """
                        SELECT count(*) FROM status_change
                        WHERE service_order_id = ? AND to_status = 'AWAITING_APPROVAL'
                        """,
                        Long.class,
                        serviceOrderId))
                .isOne();
    }

    @Test
    void shouldReportAnUnknownServiceOrder() throws Exception {
        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/sendQuote", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private Long diagnoseWithAService() throws Exception {
        as(Role.MECHANIC, startDiagnosis()).andExpect(status().isOk());
        String body = as(Role.MECHANIC, addService(savedService()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
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

    private Long savedService() {
        return serviceOfferingRepository
                .save(new ServiceOffering("Freios", null, new Money(new BigDecimal("150.00")), 60))
                .getId();
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT from_status, to_status, changed_by_user_id, date_created FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private MockHttpServletRequestBuilder startDiagnosis() {
        return post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId);
    }

    private MockHttpServletRequestBuilder sendQuote() {
        return post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId);
    }

    private MockHttpServletRequestBuilder addService(Long serviceOfferingId) {
        return post("/admin/serviceOrders/{id}/services", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"serviceOfferingId": %d, "quantity": 2}
                        """.formatted(serviceOfferingId));
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
