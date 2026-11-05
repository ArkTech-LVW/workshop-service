package br.com.arktech.workshop.inventory.api.part;

import static br.com.arktech.workshop.TestUsers.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class PartManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static Stream<Arguments> partEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/parts")),
                Arguments.of(get("/admin/parts/lowStock")),
                Arguments.of(get("/admin/parts/1")),
                Arguments.of(put("/admin/parts/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/parts/1")));
    }

    static Stream<Arguments> partWriteEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(put("/admin/parts/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/parts/1")));
    }

    @ParameterizedTest
    @MethodSource("partEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("partWriteEndpoints")
    void shouldDenyWritesToUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(Role.ATTENDANT, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ATTENDANT", "MECHANIC"})
    void shouldLetAttendantsAndMechanicsConsultParts(Role role) throws Exception {
        Integer id = createPart(uniqueSku());

        mockMvc.perform(get("/admin/parts").with(bearer(role)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/parts/lowStock").with(bearer(role)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/parts/{id}", id).with(bearer(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void shouldCreateFindAndSearchPart() throws Exception {
        String sku = uniqueSku();
        String name = "Óleo 5W30 " + sku;

        String created = mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "%s", "type": "SUPPLY", "unit": "LITER", "salePrice": 45.9,
                         "minimumStock": 10, "stockQuantity": 99}
                        """.formatted(sku.toLowerCase(), name)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.salePrice").value(45.90))
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.minimumStock").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/parts/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SUPPLY"))
                .andExpect(jsonPath("$.unit").value("LITER"));
        mockMvc.perform(get("/admin/parts").param("sku", sku.toLowerCase()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get("/admin/parts").param("name", sku).param("type", "SUPPLY").with(bearer(Role.ADMIN)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/admin/parts").param("name", sku).param("type", "PART").with(bearer(Role.ADMIN)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectDuplicatedSkuIgnoringCase() throws Exception {
        String sku = uniqueSku();
        mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(partJson(sku))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partJson(sku.toLowerCase()))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("part.skuAlreadyExists"));
    }

    @Test
    void shouldUpdatePartWithoutChangingStockQuantity() throws Exception {
        Integer id = createPart(uniqueSku());
        jdbcTemplate.update("UPDATE part SET stock_quantity = 7 WHERE id = ?", id);
        String newSku = uniqueSku();

        mockMvc.perform(put("/admin/parts/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de ar", "type": "PART", "unit": "UNIT", "salePrice": 58.5,
                         "minimumStock": 3, "stockQuantity": 99}
                        """.formatted(newSku.toLowerCase())).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(newSku))
                .andExpect(jsonPath("$.name").value("Filtro de ar"))
                .andExpect(jsonPath("$.salePrice").value(58.50))
                .andExpect(jsonPath("$.minimumStock").value(3))
                .andExpect(jsonPath("$.stockQuantity").value(7))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(7));
        mockMvc.perform(get("/admin/parts/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(jsonPath("$.stockQuantity").value(7));
    }

    @Test
    void shouldRejectSkuOfAnotherPartOnUpdate() throws Exception {
        String taken = uniqueSku();
        createPart(taken);
        Integer id = createPart(uniqueSku());

        mockMvc.perform(put("/admin/parts/{id}", id).contentType(MediaType.APPLICATION_JSON).content(partJson(taken))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("part.skuAlreadyExists"));
    }

    @Test
    void shouldDeletePart() throws Exception {
        Integer id = createPart(uniqueSku());

        mockMvc.perform(delete("/admin/parts/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isNoContent());
        mockMvc.perform(get("/admin/parts/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
    }

    @Test
    void shouldKeepPartThatIsStillReferenced() throws Exception {
        Integer id = createPart(uniqueSku());
        jdbcTemplate.execute("CREATE TABLE part_reference_test (part_id BIGINT REFERENCES part (id))");
        try {
            jdbcTemplate.update("INSERT INTO part_reference_test (part_id) VALUES (?)", id);

            mockMvc.perform(delete("/admin/parts/{id}", id).with(bearer(Role.ADMIN)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("part.inUse"));
            mockMvc.perform(get("/admin/parts/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isOk());
        } finally {
            jdbcTemplate.execute("DROP TABLE part_reference_test");
        }
    }

    @Test
    void shouldAnswer404ForUnknownPart() throws Exception {
        mockMvc.perform(get("/admin/parts/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
        mockMvc.perform(put("/admin/parts/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partJson(uniqueSku()))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
        mockMvc.perform(delete("/admin/parts/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
    }

    private Integer createPart(String sku) throws Exception {
        String created = mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partJson(sku))
                        .with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private static String partJson(String sku) {
        return """
                {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                 "minimumStock": 2}
                """.formatted(sku);
    }

    private static String uniqueSku() {
        return "IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
