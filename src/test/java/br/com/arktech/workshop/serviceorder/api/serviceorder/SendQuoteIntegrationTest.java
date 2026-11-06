package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class SendQuoteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    private Long userId;

    private Long serviceOrderId;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        userId = testUsers.save(Role.ATTENDANT).getId();
        serviceOrderId = openServiceOrder();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(sendQuote()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSendTheQuoteRecordingWhoSentIt() throws Exception {
        diagnoseWithAService();

        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT)))
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
        mockMvc.perform(sendQuote().with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

        serviceOrderId = openServiceOrder();
        diagnoseWithAService();
        mockMvc.perform(sendQuote().with(bearer(userId, Role.ADMIN))).andExpect(status().isOk());
    }

    @Test
    void shouldLockTheItemsAfterSending() throws Exception {
        Long serviceItemId = diagnoseWithAService();
        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());

        mockMvc.perform(addService(savedService()).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));
        mockMvc.perform(delete("/admin/serviceOrders/{id}/services/{itemId}", serviceOrderId, serviceItemId)
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM service_item WHERE service_order_id = ?", Long.class, serviceOrderId))
                .isOne();
    }

    @Test
    void shouldRefuseAQuoteWithoutItemsWithoutChangingTheServiceOrder() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());

        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.quoteWithoutItems"));

        assertThat(statusOf()).isEqualTo("IN_DIAGNOSIS");
        assertThat(lastStatusChange()).containsEntry("to_status", "IN_DIAGNOSIS");
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotInDiagnosis() throws Exception {
        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(statusOf()).isEqualTo("RECEIVED");
    }

    @Test
    void shouldRefuseToSendTheQuoteTwice() throws Exception {
        diagnoseWithAService();
        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT))).andExpect(status().isOk());

        mockMvc.perform(sendQuote().with(bearer(userId, Role.ATTENDANT)))
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
        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", Long.MAX_VALUE)
                        .with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private Long diagnoseWithAService() throws Exception {
        mockMvc.perform(startDiagnosis().with(bearer(userId, Role.MECHANIC))).andExpect(status().isOk());
        String body = mockMvc.perform(addService(savedService()).with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
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
}
