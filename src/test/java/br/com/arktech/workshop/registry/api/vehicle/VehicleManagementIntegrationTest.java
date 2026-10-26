package br.com.arktech.workshop.registry.api.vehicle;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.security.Role;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;

import com.jayway.jsonpath.JsonPath;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class VehicleManagementIntegrationTest {

    private static final String LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ";
    private static final String VIN_CHARACTERS = LETTERS + "0123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    static Stream<Arguments> vehicleEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/vehicles/1")));
    }

    @ParameterizedTest
    @MethodSource("vehicleEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("vehicleEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenFor(Role.ATTENDANT, Role.MECHANIC);

        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldCreateAndFindVehicle() throws Exception {
        Integer customerId = createCustomer();
        String plate = uniquePlate();
        String chassis = uniqueChassis();

        String created = asAdmin(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": %d, "licensePlate": "%s", "chassis": "%s", "brand": "Volkswagen",
                         "model": "Gol", "year": 2020, "color": "Prata"}
                        """.formatted(customerId, withHyphen(plate).toLowerCase(), chassis.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.chassis").value(chassis))
                .andExpect(jsonPath("$.year").value(2020))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        asAdmin(get("/admin/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.model").value("Gol"));
    }

    @Test
    void shouldRequireExistingCustomer() throws Exception {
        asAdmin(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(Long.MAX_VALUE, uniquePlate(), null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
    }

    @Test
    void shouldRejectDuplicatedLicensePlateAndChassis() throws Exception {
        Integer customerId = createCustomer();
        String plate = uniquePlate();
        String chassis = uniqueChassis();
        createVehicle(customerId, plate, chassis);

        asAdmin(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, withHyphen(plate), null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.licensePlateAlreadyExists"));
        asAdmin(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, uniquePlate(), chassis)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("vehicle.chassisAlreadyExists"));
    }

    @Test
    void shouldRejectInvalidPlateChassisAndYear() throws Exception {
        Integer customerId = createCustomer();

        asAdmin(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": %d, "licensePlate": "AB-12345", "chassis": "9BWZZZ370VT004251",
                         "brand": "Fiat", "model": "147", "year": 1980, "color": "Branco"}
                        """.formatted(customerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field").value(containsInAnyOrder("licensePlate", "chassis", "year")));
    }

    @Test
    void shouldAnswer404ForUnknownVehicle() throws Exception {
        asAdmin(get("/admin/vehicles/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("vehicle.notFound"));
    }

    @Test
    void shouldKeepCustomerThatHasVehicles() throws Exception {
        Integer customerId = createCustomer();
        createVehicle(customerId, uniquePlate(), null);

        asAdmin(delete("/admin/customers/{id}", customerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer.inUse"));
        asAdmin(get("/admin/customers/{id}", customerId)).andExpect(status().isOk());
    }

    private Integer createCustomer() throws Exception {
        String body = asAdmin(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(uniqueCPF().value())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Integer createVehicle(long customerId, String licensePlate, @Nullable String chassis) throws Exception {
        String body = asAdmin(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(customerId, licensePlate, chassis)))
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

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ADMIN)));
    }

    private String tokenFor(Role... roles) {
        return tokenIssuer.issue(1L, Set.of(roles)).value();
    }

    private static String uniquePlate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "" + randomOf(LETTERS) + randomOf(LETTERS) + randomOf(LETTERS) + random.nextInt(10)
                + randomOf(LETTERS) + "%02d".formatted(random.nextInt(100));
    }

    private static String withHyphen(String licensePlate) {
        return licensePlate.substring(0, 3) + "-" + licensePlate.substring(3);
    }

    private static String uniqueChassis() {
        String base = IntStream.range(0, 16)
                .mapToObj(index -> String.valueOf(randomOf(VIN_CHARACTERS)))
                .reduce("", String::concat);
        return "0123456789X".chars()
                .mapToObj(checkDigit -> base.substring(0, 8) + (char) checkDigit + base.substring(8))
                .filter(Chassis::isValid)
                .findFirst()
                .orElseThrow();
    }

    private static char randomOf(String characters) {
        return characters.charAt(ThreadLocalRandom.current().nextInt(characters.length()));
    }

    private static CPFCNPJ uniqueCPF() {
        String base = "%09d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000L));
        return IntStream.range(0, 100)
                .mapToObj(checkDigits -> base + "%02d".formatted(checkDigits))
                .filter(CPFCNPJ::isValid)
                .findFirst()
                .map(CPFCNPJ::of)
                .orElseThrow();
    }
}
