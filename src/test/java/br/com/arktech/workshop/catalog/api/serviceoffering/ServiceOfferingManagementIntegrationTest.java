package br.com.arktech.workshop.catalog.api.serviceoffering;

import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ServiceOfferingManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    static Stream<Arguments> serviceOfferingEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/services")),
                Arguments.of(get("/admin/services/1")),
                Arguments.of(put("/admin/services/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(post("/admin/services/1/activate")),
                Arguments.of(post("/admin/services/1/deactivate")));
    }

    @ParameterizedTest
    @MethodSource("serviceOfferingEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("serviceOfferingEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenFor(Role.ATTENDANT, Role.MECHANIC);

        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldCreateFindAndSearchServiceOfferingByName() throws Exception {
        String name = uniqueName("Troca de óleo");

        String created = asAdmin(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "description": "Óleo e filtro", "laborPrice": 150.5, "estimatedMinutes": 60}
                        """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.laborPrice").value(150.50))
                .andExpect(jsonPath("$.estimatedMinutes").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        asAdmin(get("/admin/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Óleo e filtro"));
        asAdmin(get("/admin/services").param("name", name.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        asAdmin(get("/admin/services").param("name", uniqueName("Inexistente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldUpdateDataKeepingActiveStatus() throws Exception {
        Integer id = createServiceOffering(uniqueName("Alinhamento"));
        String renamed = uniqueName("Alinhamento 3D");

        asAdmin(put("/admin/services/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "laborPrice": 99.90, "estimatedMinutes": 45}
                        """.formatted(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.laborPrice").value(99.90))
                .andExpect(jsonPath("$.estimatedMinutes").value(45))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldDeactivateAndActivateServiceOffering() throws Exception {
        String name = uniqueName("Balanceamento");
        Integer id = createServiceOffering(name);

        asAdmin(post("/admin/services/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        asAdmin(get("/admin/services").param("name", name).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        asAdmin(get("/admin/services").param("name", name).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        asAdmin(post("/admin/services/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectInvalidLaborPrice() throws Exception {
        asAdmin(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Troca de óleo", "laborPrice": -10, "estimatedMinutes": 60}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("laborPrice"));
    }

    @Test
    void shouldAnswer404ForUnknownServiceOffering() throws Exception {
        asAdmin(get("/admin/services/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOffering.notFound"));
        asAdmin(post("/admin/services/{id}/deactivate", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOffering.notFound"));
    }

    private Integer createServiceOffering(String name) throws Exception {
        String body = asAdmin(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "laborPrice": 80, "estimatedMinutes": 30}
                        """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ADMIN)));
    }

    private String tokenFor(Role... roles) {
        return tokenIssuer.issue(1L, Set.of(roles)).value();
    }

    private static String uniqueName(String name) {
        return name + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
