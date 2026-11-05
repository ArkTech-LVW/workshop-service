package br.com.arktech.workshop.registry.api.vehicle;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniqueChassis;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static br.com.arktech.workshop.UniqueTestData.withHyphen;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class VehicleManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> vehicleEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/vehicles")),
                Arguments.of(get("/admin/vehicles/1")),
                Arguments.of(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/vehicles/1")),
                Arguments.of(get("/admin/customers/1/vehicles")));
    }

    static Stream<Arguments> vehicleEndpointsClosedToMechanics() {
        return Stream.of(
                Arguments.of(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/vehicles/1")),
                Arguments.of(get("/admin/customers/1/vehicles")));
    }

    @ParameterizedTest
    @MethodSource("vehicleEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("vehicleEndpointsClosedToMechanics")
    void shouldDenyMechanics(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldLetMechanicsConsultVehicles() throws Exception {
        Integer id = createVehicle(createCustomer(), uniquePlate(), null);

        mockMvc.perform(get("/admin/vehicles").with(bearer(Role.MECHANIC)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/vehicles/{id}", id).with(bearer(Role.MECHANIC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void shouldLetAttendantsCreateAndFindVehicles() throws Exception {
        Integer customerId = createCustomer();

        String created = mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, uniquePlate(), null))
                        .with(bearer(Role.ATTENDANT)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/vehicles/{id}", id).with(bearer(Role.ATTENDANT))).andExpect(status().isOk());
    }

    @Test
    void shouldLetAttendantsListVehicles() throws Exception {
        Integer customerId = createCustomer();
        createVehicle(customerId, uniquePlate(), null);

        mockMvc.perform(get("/admin/vehicles").with(bearer(Role.ATTENDANT))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/customers/{id}/vehicles", customerId).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldLetAttendantsUpdateButNotDeleteVehicles() throws Exception {
        String plate = uniquePlate();
        Integer id = createVehicle(createCustomer(), plate, null);

        mockMvc.perform(put("/admin/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson(plate, null, "Azul"))
                        .with(bearer(Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("Azul"));
        mockMvc.perform(delete("/admin/vehicles/{id}", id).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
        mockMvc.perform(get("/admin/vehicles/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isOk());
    }

    @Test
    void shouldCreateAndFindVehicle() throws Exception {
        Integer customerId = createCustomer();
        String plate = uniquePlate();
        String chassis = uniqueChassis();

        String created = mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": %d, "licensePlate": "%s", "chassis": "%s", "brand": "Volkswagen",
                         "model": "Gol", "year": 2020, "color": "Prata"}
                        """.formatted(customerId, withHyphen(plate).toLowerCase(), chassis.toLowerCase()))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.chassis").value(chassis))
                .andExpect(jsonPath("$.year").value(2020))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/vehicles/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.model").value("Gol"));
    }

    @Test
    void shouldSearchByLicensePlateAndListVehiclesOfCustomer() throws Exception {
        Integer ana = createCustomer();
        Integer bia = createCustomer();
        String plate = uniquePlate();
        Integer id = createVehicle(ana, plate, null);
        createVehicle(bia, uniquePlate(), null);

        mockMvc.perform(get("/admin/vehicles").param("licensePlate", withHyphen(plate).toLowerCase())
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get("/admin/vehicles").param("licensePlate", uniquePlate()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/admin/customers/{id}/vehicles", ana).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void shouldListNoVehiclesForCustomerWithoutVehicles() throws Exception {
        mockMvc.perform(get("/admin/customers/{id}/vehicles", createCustomer()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRequireExistingCustomer() throws Exception {
        mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(Long.MAX_VALUE, uniquePlate(), null))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
        mockMvc.perform(get("/admin/customers/{id}/vehicles", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
    }

    @Test
    void shouldRejectDuplicatedLicensePlateAndChassis() throws Exception {
        Integer customerId = createCustomer();
        String plate = uniquePlate();
        String chassis = uniqueChassis();
        createVehicle(customerId, plate, chassis);

        mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, withHyphen(plate), null))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.licensePlateAlreadyExists"));
        mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, uniquePlate(), chassis))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.chassisAlreadyExists"));
    }

    @Test
    void shouldRejectInvalidPlateChassisAndYear() throws Exception {
        Integer customerId = createCustomer();

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": %d, "licensePlate": "AB-12345", "chassis": "9BWZZZ370VT004251",
                         "brand": "Fiat", "model": "147", "year": 1980, "color": "Branco"}
                        """.formatted(customerId)).with(bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field").value(containsInAnyOrder("licensePlate", "chassis", "year")));
    }

    @Test
    void shouldAnswer404ForUnknownVehicle() throws Exception {
        mockMvc.perform(get("/admin/vehicles/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("vehicle.notFound"));
    }

    @Test
    void shouldRegisterChassisLaterRepaintAndClearChassis() throws Exception {
        Integer customerId = createCustomer();
        String plate = uniquePlate();
        String chassis = uniqueChassis();
        Integer id = createVehicle(customerId, plate, null);

        mockMvc.perform(put("/admin/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson(plate, chassis, "Azul"))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chassis").value(chassis))
                .andExpect(jsonPath("$.color").value("Azul"))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.model").value("Uno"))
                .andExpect(jsonPath("$.year").value(2010));
        mockMvc.perform(put("/admin/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson(plate, null, "Azul"))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chassis").value(nullValue()));
    }

    @Test
    void shouldRejectUpdateToAnotherVehiclesPlate() throws Exception {
        Integer customerId = createCustomer();
        String taken = uniquePlate();
        createVehicle(customerId, taken, null);
        Integer id = createVehicle(customerId, uniquePlate(), null);

        mockMvc.perform(put("/admin/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson(withHyphen(taken), null, "Branco"))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.licensePlateAlreadyExists"));
    }

    @Test
    void shouldDeleteVehicle() throws Exception {
        Integer id = createVehicle(createCustomer(), uniquePlate(), null);

        mockMvc.perform(delete("/admin/vehicles/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isNoContent());
        mockMvc.perform(get("/admin/vehicles/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("vehicle.notFound"));
    }

    @Test
    void shouldKeepCustomerThatHasVehicles() throws Exception {
        Integer customerId = createCustomer();
        createVehicle(customerId, uniquePlate(), null);

        mockMvc.perform(delete("/admin/customers/{id}", customerId).with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer.inUse"));
        mockMvc.perform(get("/admin/customers/{id}", customerId).with(bearer(Role.ADMIN))).andExpect(status().isOk());
    }

    private Integer createCustomer() throws Exception {
        String body = mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(uniqueCPF().value())).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Integer createVehicle(long customerId, String licensePlate, @Nullable String chassis) throws Exception {
        String body = mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, licensePlate, chassis))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String vehicleJson(long customerId, String licensePlate, @Nullable String chassis) {
        return """
                {"customerId": %d, "licensePlate": "%s", "chassis": %s, "brand": "Fiat", "model": "Uno",
                 "year": 2010, "color": "Branco"}
                """.formatted(customerId, licensePlate, chassis == null ? "null" : "\"" + chassis + "\"");
    }

    private static String updateJson(String licensePlate, @Nullable String chassis, String color) {
        return """
                {"licensePlate": "%s", "chassis": %s, "color": "%s"}
                """.formatted(licensePlate, chassis == null ? "null" : "\"" + chassis + "\"", color);
    }
}
