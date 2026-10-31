package br.com.arktech.workshop.inventory.api.part;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class PartManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static Stream<Arguments> partEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/parts")),
                Arguments.of(get("/admin/parts/1")),
                Arguments.of(put("/admin/parts/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/parts/1")));
    }

    @ParameterizedTest
    @MethodSource("partEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("partEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenFor(Role.ATTENDANT, Role.MECHANIC);

        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldCreateFindAndSearchPart() throws Exception {
        String sku = uniqueSku();
        String name = "Óleo 5W30 " + sku;

        String created = asAdmin(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "%s", "type": "SUPPLY", "unit": "LITER", "salePrice": 45.9,
                         "minimumStock": 10, "stockQuantity": 99}
                        """.formatted(sku.toLowerCase(), name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.salePrice").value(45.90))
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.minimumStock").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        asAdmin(get("/admin/parts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SUPPLY"))
                .andExpect(jsonPath("$.unit").value("LITER"));
        asAdmin(get("/admin/parts").param("sku", sku.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        asAdmin(get("/admin/parts").param("name", sku).param("type", "SUPPLY"))
                .andExpect(jsonPath("$.totalElements").value(1));
        asAdmin(get("/admin/parts").param("name", sku).param("type", "PART"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectDuplicatedSkuIgnoringCase() throws Exception {
        String sku = uniqueSku();
        asAdmin(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(partJson(sku)))
                .andExpect(status().isCreated());

        asAdmin(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(partJson(sku.toLowerCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("part.skuAlreadyExists"));
    }

    @Test
    void shouldUpdatePartWithoutChangingStockQuantity() throws Exception {
        Integer id = createPart(uniqueSku());
        jdbcTemplate.update("UPDATE part SET stock_quantity = 7 WHERE id = ?", id);
        String newSku = uniqueSku();

        asAdmin(put("/admin/parts/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de ar", "type": "PART", "unit": "UNIT", "salePrice": 58.5,
                         "minimumStock": 3, "stockQuantity": 99}
                        """.formatted(newSku.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(newSku))
                .andExpect(jsonPath("$.name").value("Filtro de ar"))
                .andExpect(jsonPath("$.salePrice").value(58.50))
                .andExpect(jsonPath("$.minimumStock").value(3))
                .andExpect(jsonPath("$.stockQuantity").value(7))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(7));
        asAdmin(get("/admin/parts/{id}", id)).andExpect(jsonPath("$.stockQuantity").value(7));
    }

    @Test
    void shouldRejectSkuOfAnotherPartOnUpdate() throws Exception {
        String taken = uniqueSku();
        createPart(taken);
        Integer id = createPart(uniqueSku());

        asAdmin(put("/admin/parts/{id}", id).contentType(MediaType.APPLICATION_JSON).content(partJson(taken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("part.skuAlreadyExists"));
    }

    @Test
    void shouldDeletePart() throws Exception {
        Integer id = createPart(uniqueSku());

        asAdmin(delete("/admin/parts/{id}", id)).andExpect(status().isNoContent());
        asAdmin(get("/admin/parts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
    }

    @Test
    void shouldKeepPartThatIsStillReferenced() throws Exception {
        Integer id = createPart(uniqueSku());
        jdbcTemplate.execute("CREATE TABLE part_reference_test (part_id BIGINT REFERENCES part (id))");
        try {
            jdbcTemplate.update("INSERT INTO part_reference_test (part_id) VALUES (?)", id);

            asAdmin(delete("/admin/parts/{id}", id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("part.inUse"));
            asAdmin(get("/admin/parts/{id}", id)).andExpect(status().isOk());
        } finally {
            jdbcTemplate.execute("DROP TABLE part_reference_test");
        }
    }

    @Test
    void shouldAnswer404ForUnknownPart() throws Exception {
        asAdmin(get("/admin/parts/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
        asAdmin(put("/admin/parts/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partJson(uniqueSku())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
        asAdmin(delete("/admin/parts/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
    }

    private Integer createPart(String sku) throws Exception {
        String created = asAdmin(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(partJson(sku)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ADMIN)));
    }

    private String tokenFor(Role... roles) {
        return tokenIssuer.issue(1L, Set.of(roles)).value();
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
