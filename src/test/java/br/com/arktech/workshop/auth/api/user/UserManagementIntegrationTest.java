package br.com.arktech.workshop.auth.api.user;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class UserManagementIntegrationTest {

    private static final String PASSWORD = "Sup3r-Secret!";

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(request.with(bearer(Role.ATTENDANT, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldManageUserLifecycle() throws Exception {
        String email = uniqueEmail();

        String created = mockMvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "email": "%s", "password": "%s", "roles": ["MECHANIC", "ATTENDANT"]}
                        """.formatted(email.toUpperCase(), PASSWORD)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles").value(contains("ATTENDANT", "MECHANIC")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(created).doesNotContain(PASSWORD).doesNotContainIgnoringCase("password");
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/users/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));
        mockMvc.perform(get("/admin/users").param("size", "100").with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[*].password").doesNotExist());

        mockMvc.perform(put("/admin/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza", "roles": ["MECHANIC"]}
                        """).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.roles").value(contains("MECHANIC")));

        mockMvc.perform(post("/admin/users/{id}/deactivate", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        login(email, PASSWORD).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/users/{id}/activate", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldRejectDuplicatedEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        mockMvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Bia", "email": "%s", "password": "%s", "roles": ["MECHANIC"]}
                        """.formatted(email.toUpperCase(), PASSWORD)).with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.emailAlreadyExists"));
    }

    @Test
    void shouldAnswer404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/admin/users/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("user.notFound"));
    }

    @Test
    void shouldLetUserChangeOwnPassword() throws Exception {
        String email = uniqueEmail();
        Integer id = createUser(email);

        mockMvc.perform(put("/auth/password")
                        .with(bearer(id.longValue(), Role.MECHANIC))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "Another-Secret!"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "Another-Secret!").andExpect(status().isOk());
    }

    @Test
    void shouldRequireTokenToChangePassword() throws Exception {
        mockMvc.perform(put("/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"currentPassword": "%s", "newPassword": "Another-Secret!"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private Integer createUser(String email) throws Exception {
        String body = mockMvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "email": "%s", "password": "%s", "roles": ["MECHANIC"]}
                        """.formatted(email, PASSWORD)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password)));
    }
}
