package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
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
class CustomerServiceOrderIntegrationTest {

    private static final String CUSTOMER_DOCUMENT = "X-Customer-Document";

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

    private CPFCNPJ document;

    private String plate;

    private Long serviceOrderId;

    private String publicId;

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
        document = createCustomer();
        plate = uniquePlate();
        String body = as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), plate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        serviceOrderId = ((Number) JsonPath.read(body, "$.id")).longValue();
        publicId = JsonPath.read(body, "$.publicId");
    }

    @Test
    void shouldShowTheServiceOrderToItsCustomerWithoutAToken() throws Exception {
        mockMvc.perform(track(document.formatted()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.problemDescription").value("Barulho ao frear"))
                .andExpect(jsonPath("$.vehicle.licensePlate").value(plate))
                .andExpect(jsonPath("$.vehicle.model").value("Gol"))
                .andExpect(jsonPath("$.statusHistory[*].status", contains("RECEIVED")))
                .andExpect(jsonPath("$.statusHistory[*].date", everyItem(notNullValue())))
                .andExpect(jsonPath("$.quote").isEmpty())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.vehicleId").doesNotExist())
                .andExpect(jsonPath("$.mechanicId").doesNotExist())
                .andExpect(jsonPath("$.statusHistory[0].changedByUserId").doesNotExist());
    }

    @Test
    void shouldAnswer404ForAnUnknownPublicIdOrTheDocumentOfAnotherCustomer() throws Exception {
        String unknown = UUID.randomUUID().toString();
        mockMvc.perform(get("/customer/serviceOrders/{publicId}", unknown).header(CUSTOMER_DOCUMENT, document.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"))
                .andExpect(jsonPath("$.message").value("Ordem de serviço %s não encontrada.".formatted(unknown)));

        mockMvc.perform(track(createCustomer().value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"))
                .andExpect(jsonPath("$.message").value("Ordem de serviço %s não encontrada.".formatted(publicId)));
        mockMvc.perform(approve(uniqueCPF().value())).andExpect(status().isNotFound());
        assertThat(statusOf()).isEqualTo("RECEIVED");
    }

    @Test
    void shouldShowTheDetailedQuoteOnlyAfterItIsSent() throws Exception {
        Long brakes = createService("Troca de pastilhas", "150.00");
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        addItem("services", "serviceOfferingId", brakes, 1);
        addItem("parts", "partId", pad, 2);

        mockMvc.perform(track(document.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))
                .andExpect(jsonPath("$.quote").isEmpty());

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId))
                .andExpect(status().isOk());

        mockMvc.perform(track(document.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$.statusHistory[*].status", contains("RECEIVED", "IN_DIAGNOSIS", "AWAITING_APPROVAL")))
                .andExpect(jsonPath("$.quote.services[0].name").value("Troca de pastilhas"))
                .andExpect(jsonPath("$.quote.services[0].quantity").value(1))
                .andExpect(jsonPath("$.quote.services[0].subtotal").value(150.00))
                .andExpect(jsonPath("$.quote.parts[0].name").value("Pastilha de freio"))
                .andExpect(jsonPath("$.quote.parts[0].quantity").value(2))
                .andExpect(jsonPath("$.quote.parts[0].unitPrice").value(45.90))
                .andExpect(jsonPath("$.quote.parts[0].subtotal").value(91.80))
                .andExpect(jsonPath("$.quote.totalPrice").value(241.80))
                .andExpect(jsonPath("$.quote.parts[0].id").doesNotExist())
                .andExpect(jsonPath("$.quote.parts[0].partId").doesNotExist());
    }

    @Test
    void shouldApproveWithoutRecordingAUserEvenWhenAStaffTokenIsSent() throws Exception {
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        sendQuoteWithPart(pad, 3);

        mockMvc.perform(approve(document.value())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ATTENDANT)))
                .andExpect(status().isNoContent());

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
        assertThat(lastStatusChange())
                .containsEntry("to_status", "QUOTE_APPROVED")
                .containsEntry("changed_by_user_id", null);
        assertThat(jdbcTemplate.queryForObject("SELECT stock_quantity FROM part WHERE id = ?", Integer.class, pad))
                .isEqualTo(7);
        assertThat(jdbcTemplate.queryForList(
                        "SELECT quantity, created_by_user_id FROM stock_movement WHERE service_order_id = ?",
                        serviceOrderId))
                .singleElement()
                .satisfies(withdrawal ->
                        assertThat(withdrawal).containsEntry("quantity", 3).containsEntry("created_by_user_id", null));

        mockMvc.perform(approve(document.value()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
    }

    @Test
    void shouldRejectRecordingTheCustomersReason() throws Exception {
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        sendQuoteWithPart(pad, 1);

        mockMvc.perform(post("/customer/serviceOrders/{publicId}/reject", publicId)
                        .header(CUSTOMER_DOCUMENT, document.formatted())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Achei o valor alto"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(statusOf()).isEqualTo("QUOTE_REJECTED");
        assertThat(lastStatusChange())
                .containsEntry("to_status", "QUOTE_REJECTED")
                .containsEntry("changed_by_user_id", null)
                .containsEntry("description", "Achei o valor alto");
        mockMvc.perform(track(document.value()))
                .andExpect(jsonPath("$.statusHistory[3].status").value("QUOTE_REJECTED"))
                .andExpect(jsonPath("$.statusHistory[3].description").value("Achei o valor alto"))
                .andExpect(jsonPath("$.quote.parts[0].name").value("Pastilha de freio"));
    }

    private void sendQuoteWithPart(Long partId, int quantity) throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        addItem("parts", "partId", partId, quantity);
        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId))
                .andExpect(status().isOk());
    }

    private void addItem(String kind, String idField, Long id, int quantity) throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/" + kind, serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"%s": %d, "quantity": %d}
                                """.formatted(idField, id, quantity)))
                .andExpect(status().isCreated());
    }

    private CPFCNPJ createCustomer() throws Exception {
        CPFCNPJ customerDocument = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(customerDocument.value())))
                .andExpect(status().isCreated());
        return customerDocument;
    }

    private Long createService(String name, String laborPrice) throws Exception {
        String body = as(Role.ADMIN, post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "laborPrice": %s, "estimatedMinutes": 60}
                        """.formatted(name, laborPrice)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private Long createPart(String name, String salePrice, int stockQuantity) throws Exception {
        String sku = "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = as(Role.ADMIN, post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "%s", "type": "PART", "unit": "UNIT", "salePrice": %s,
                         "minimumStock": 1}
                        """.formatted(sku, name, salePrice)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long partId = ((Number) JsonPath.read(body, "$.id")).longValue();
        as(Role.ADMIN, post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT to_status, changed_by_user_id, description FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private MockHttpServletRequestBuilder track(String customerDocument) {
        return get("/customer/serviceOrders/{publicId}", publicId).header(CUSTOMER_DOCUMENT, customerDocument);
    }

    private MockHttpServletRequestBuilder approve(String customerDocument) {
        return post("/customer/serviceOrders/{publicId}/approve", publicId).header(CUSTOMER_DOCUMENT, customerDocument);
    }

    private String tokenFor(Role role) {
        return tokenIssuer.issue(userId, Set.of(role)).value();
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(role)));
    }
}
