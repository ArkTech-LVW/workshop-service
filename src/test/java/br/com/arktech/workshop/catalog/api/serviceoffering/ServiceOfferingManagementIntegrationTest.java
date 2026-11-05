package br.com.arktech.workshop.catalog.api.serviceoffering;

import static br.com.arktech.workshop.TestUsers.bearer;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ServiceOfferingManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> serviceOfferingEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/services")),
                Arguments.of(get("/admin/services/1")),
                Arguments.of(put("/admin/services/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(post("/admin/services/1/activate")),
                Arguments.of(post("/admin/services/1/deactivate")));
    }

    static Stream<Arguments> serviceOfferingWriteEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("{}")),
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
    @MethodSource("serviceOfferingWriteEndpoints")
    void shouldDenyWritesToUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(Role.ATTENDANT, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ATTENDANT", "MECHANIC"})
    void shouldLetAttendantsAndMechanicsConsultTheCatalog(Role role) throws Exception {
        Integer id = createServiceOffering(uniqueName("Alinhamento"));

        mockMvc.perform(get("/admin/services").with(bearer(role)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/services/{id}", id).with(bearer(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void shouldCreateFindAndSearchServiceOfferingByName() throws Exception {
        String name = uniqueName("Troca de óleo");

        String created = mockMvc.perform(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "description": "Óleo e filtro", "laborPrice": 150.5, "estimatedMinutes": 60}
                        """.formatted(name)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.laborPrice").value(150.50))
                .andExpect(jsonPath("$.estimatedMinutes").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/services/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Óleo e filtro"));
        mockMvc.perform(get("/admin/services").param("name", name.toUpperCase()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get("/admin/services").param("name", uniqueName("Inexistente")).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldUpdateDataKeepingActiveStatus() throws Exception {
        Integer id = createServiceOffering(uniqueName("Alinhamento"));
        String renamed = uniqueName("Alinhamento 3D");

        mockMvc.perform(put("/admin/services/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "laborPrice": 99.90, "estimatedMinutes": 45}
                        """.formatted(renamed)).with(bearer(Role.ADMIN)))
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

        mockMvc.perform(post("/admin/services/{id}/deactivate", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/admin/services").param("name", name).param("active", "true").with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/admin/services").param("name", name).param("active", "false").with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        mockMvc.perform(post("/admin/services/{id}/activate", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectInvalidLaborPrice() throws Exception {
        mockMvc.perform(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Troca de óleo", "laborPrice": -10, "estimatedMinutes": 60}
                        """).with(bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("laborPrice"));
    }

    @Test
    void shouldAnswer404ForUnknownServiceOffering() throws Exception {
        mockMvc.perform(get("/admin/services/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOffering.notFound"));
        mockMvc.perform(post("/admin/services/{id}/deactivate", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOffering.notFound"));
    }

    private Integer createServiceOffering(String name) throws Exception {
        String body = mockMvc.perform(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "laborPrice": 80, "estimatedMinutes": 30}
                        """.formatted(name)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String uniqueName(String name) {
        return name + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
