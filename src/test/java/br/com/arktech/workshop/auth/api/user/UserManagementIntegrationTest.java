package br.com.arktech.workshop.auth.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
class UserManagementIntegrationTest {

    private static final String PASSWORD = "Sup3r-Secret!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    static Stream<Arguments> userEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/users")),
                Arguments.of(get("/admin/users/1")),
                Arguments.of(put("/admin/users/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(post("/admin/users/1/activate")),
                Arguments.of(post("/admin/users/1/deactivate")));
    }

    @ParameterizedTest
    @MethodSource("userEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("userEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenFor(1L, Role.ATTENDANT, Role.MECHANIC);

        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldManageUserLifecycle() throws Exception {
        String email = uniqueEmail();

        String created = asAdmin(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "email": "%s", "password": "%s", "roles": ["MECHANIC", "ATTENDANT"]}
                        """.formatted(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles").value(contains("ATTENDANT", "MECHANIC")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(created).doesNotContain(PASSWORD).doesNotContainIgnoringCase("password");
        Integer id = JsonPath.read(created, "$.id");

        asAdmin(get("/admin/users/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana"));
        asAdmin(get("/admin/users").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[*].password").doesNotExist());

        asAdmin(put("/admin/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza", "roles": ["MECHANIC"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.roles").value(contains("MECHANIC")));

        asAdmin(post("/admin/users/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        login(email, PASSWORD).andExpect(status().isUnauthorized());

        asAdmin(post("/admin/users/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectDuplicatedEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        asAdmin(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Bia", "email": "%s", "password": "%s", "roles": ["MECHANIC"]}
                        """.formatted(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.emailAlreadyExists"));
    }

    @Test
    void shouldAnswer404ForUnknownUser() throws Exception {
        asAdmin(get("/admin/users/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("user.notFound"));
    }

    private Integer createUser(String email) throws Exception {
        String body = asAdmin(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "email": "%s", "password": "%s", "roles": ["MECHANIC"]}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(1L, Role.ADMIN)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password)));
    }

    private String tokenFor(Long userId, Role... roles) {
        return tokenIssuer.issue(userId, Set.of(roles)).value();
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@workshop.com";
    }
}
