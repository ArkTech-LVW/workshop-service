package br.com.arktech.workshop.auth.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class EndpointAuthorizationTest {

    private static final String ACCESS_DENIED = """
            {"code": "auth.accessDenied", "message": "Você não tem permissão para acessar este recurso."}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldDenyUndeclaredEndpointToEveryRole(Role role) throws Exception {
        mockMvc.perform(as(get("/test/security/undeclared"), role))
                .andExpect(status().isForbidden())
                .andExpect(content().json(ACCESS_DENIED, JsonCompareMode.STRICT));
    }

    @Test
    void shouldRequireTokenOnUndeclaredEndpoint() throws Exception {
        mockMvc.perform(get("/test/security/undeclared")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldOpenPublicEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/test/security/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void shouldRequireTokenOnRoleEndpoint() throws Exception {
        mockMvc.perform(get("/test/security/frontDesk")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "ATTENDANT"})
    void shouldOpenRoleEndpointToAllowedRoles(Role role) throws Exception {
        mockMvc.perform(as(get("/test/security/frontDesk"), role))
                .andExpect(status().isOk())
                .andExpect(content().string("frontDesk"));
    }

    @Test
    void shouldDenyRoleEndpointToOtherRoles() throws Exception {
        mockMvc.perform(as(get("/test/security/frontDesk"), Role.MECHANIC))
                .andExpect(status().isForbidden())
                .andExpect(content().json(ACCESS_DENIED, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldOpenAuthenticatedEndpointToEveryRole(Role role) throws Exception {
        mockMvc.perform(as(get("/test/security/me"), role)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test/security/doesNotExist", "/doesNotExist", "/actuator/env", "/actuator/info"})
    void shouldDenyUnknownRoute(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(as(get(path), Role.ADMIN)).andExpect(status().isForbidden());
    }

    @Test
    void shouldKeep405ForWrongMethodOnExistingRoute() throws Exception {
        mockMvc.perform(post("/test/security/public")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(as(post("/test/security/frontDesk"), Role.ADMIN)).andExpect(status().isMethodNotAllowed());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, Role role) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue(1L, Set.of(role)).value());
    }
}
