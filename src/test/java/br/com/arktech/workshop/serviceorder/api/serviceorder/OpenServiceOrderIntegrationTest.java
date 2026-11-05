package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static br.com.arktech.workshop.UniqueTestData.withHyphen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.TestUsers;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class OpenServiceOrderIntegrationTest {

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
        mockMvc.perform(openRequest(uniqueCPF().value(), uniquePlate())).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyMechanics() throws Exception {
        mockMvc.perform(openRequest(uniqueCPF().value(), uniquePlate())
                        .with(bearer(userId, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldLetAttendantsOpenAServiceOrderRegisteringTheVehicle() throws Exception {
        CPFCNPJ document = uniqueCPF();
        Integer customerId = createCustomer(document);
        String plate = uniquePlate();

        String body = mockMvc.perform(openRequest(document.formatted(), withHyphen(plate))
                        .with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.number").value(matchesPattern("SO-\\d{4}-\\d{6,}")))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.mechanicId").value(nullValue()))
                .andExpect(jsonPath("$.problemDescription").value("Barulho ao frear"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.totalPrice").value(0.0))
                .andExpect(jsonPath("$.dateCreated").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> vehicle = vehicleWithPlate(plate);
        assertThat(vehicle)
                .containsEntry("customer_id", customerId.longValue())
                .containsEntry("brand", "Volkswagen")
                .containsEntry("model", "Gol");
        assertThat(((Number) JsonPath.read(body, "$.vehicleId")).longValue()).isEqualTo(vehicle.get("id"));
        assertThat(statusHistoryOf(JsonPath.read(body, "$.id")))
                .singleElement()
                .satisfies(change -> {
                    assertThat(change).containsEntry("from_status", null).containsEntry("to_status", "RECEIVED");
                    assertThat(change).containsEntry("changed_by_user_id", userId);
                });
    }

    @Test
    void shouldReuseTheVehicleTheCustomerAlreadyHas() throws Exception {
        CPFCNPJ document = uniqueCPF();
        createCustomer(document);
        String plate = uniquePlate();
        Long firstVehicleId = openAsAdmin(document.value(), plate);

        Long secondVehicleId = openAsAdmin(document.value(), plate);

        assertThat(secondVehicleId).isEqualTo(firstVehicleId);
        assertThat(vehicleCountWithPlate(plate)).isOne();
    }

    @Test
    void shouldRefuseAPlateOfAnotherCustomerWithoutSavingAnything() throws Exception {
        CPFCNPJ owner = uniqueCPF();
        createCustomer(owner);
        String plate = uniquePlate();
        openAsAdmin(owner.value(), plate);
        CPFCNPJ other = uniqueCPF();
        createCustomer(other);
        long serviceOrders = serviceOrderCount();

        mockMvc.perform(openRequest(other.value(), plate).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.licensePlateOfAnotherCustomer"));

        assertThat(serviceOrderCount()).isEqualTo(serviceOrders);
    }

    @Test
    void shouldAskToRegisterAnUnknownCustomerWithoutSavingAnything() throws Exception {
        String plate = uniquePlate();
        long serviceOrders = serviceOrderCount();

        mockMvc.perform(openRequest(uniqueCPF().value(), plate).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.customerNotRegistered"));

        assertThat(serviceOrderCount()).isEqualTo(serviceOrders);
        assertThat(vehicleCountWithPlate(plate)).isZero();
    }

    @Test
    void shouldRejectAnInvalidDocumentOrPlateWithoutSavingAnything() throws Exception {
        CPFCNPJ document = uniqueCPF();
        createCustomer(document);
        long serviceOrders = serviceOrderCount();

        mockMvc.perform(openRequest("123.456.789-00", uniquePlate()).with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("customerDocument"));
        mockMvc.perform(openRequest(document.value(), "AB-12345").with(bearer(userId, Role.ATTENDANT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("vehicle.licensePlate"));

        assertThat(serviceOrderCount()).isEqualTo(serviceOrders);
    }

    private Long openAsAdmin(String document, String plate) throws Exception {
        String body = mockMvc.perform(openRequest(document, plate).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.vehicleId")).longValue();
    }

    private Integer createCustomer(CPFCNPJ document) throws Exception {
        String body = mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())).with(bearer(userId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Map<String, Object> vehicleWithPlate(String plate) {
        return jdbcTemplate.queryForMap(
                "SELECT id, customer_id, brand, model FROM vehicle WHERE license_plate = ?", plate);
    }

    private long vehicleCountWithPlate(String plate) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM vehicle WHERE license_plate = ?", Long.class, plate);
    }

    private long serviceOrderCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM service_order", Long.class);
    }

    private List<Map<String, Object>> statusHistoryOf(Integer serviceOrderId) {
        return jdbcTemplate.queryForList(
                "SELECT from_status, to_status, changed_by_user_id FROM status_change WHERE service_order_id = ?",
                serviceOrderId.longValue());
    }

    private static MockHttpServletRequestBuilder openRequest(String document, String plate) {
        return post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerDocument": "%s",
                 "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                             "color": "Prata"},
                 "problemDescription": "Barulho ao frear"}
                """.formatted(document, plate));
    }
}
