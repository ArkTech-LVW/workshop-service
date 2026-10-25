package br.com.arktech.workshop.auth.api.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.auth.application.createuser.CreateUserCommand;
import br.com.arktech.workshop.auth.application.createuser.CreateUserUseCase;
import br.com.arktech.workshop.auth.application.finduser.FindUserUseCase;
import br.com.arktech.workshop.auth.application.listusers.ListUsersQuery;
import br.com.arktech.workshop.auth.application.listusers.ListUsersUseCase;
import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class UserControllerTest {

    private static final UserResult ANA =
            new UserResult(1L, "Ana", "ana@workshop.com", true, Set.of(Role.MECHANIC, Role.ADMIN));
    private static final String ANA_JSON = """
            {"id": 1, "name": "Ana", "email": "ana@workshop.com", "active": true, "roles": ["ADMIN", "MECHANIC"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private FindUserUseCase findUserUseCase;

    @Test
    void shouldCreateUserWithoutEchoingThePassword() throws Exception {
        when(createUserUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@workshop.com", "password": "Sup3r-Secret!",
                                 "roles": ["ADMIN", "MECHANIC"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(json(ANA_JSON))
                .andExpect(content().string(not(containsString("Sup3r"))));
        verify(createUserUseCase)
                .execute(new CreateUserCommand(
                        "Ana", "ana@workshop.com", "Sup3r-Secret!", Set.of(Role.ADMIN, Role.MECHANIC)));
    }

    @Test
    void shouldValidateNewUser() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "not-an-email", "password": "short", "roles": []}
                                """.formatted("a".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "email", "message": "E-mail inválido."},
                            {"field": "name", "message": "Deve ter no máximo 150 caracteres."},
                            {"field": "password", "message": "Deve ter entre 8 e 128 caracteres."},
                            {"field": "roles", "message": "Não pode estar vazio."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void shouldRejectUnknownRole() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@workshop.com", "password": "Sup3r-Secret!",
                                 "roles": ["OWNER"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.invalidFields", "details": [{"field": "roles[0]"}]}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldAnswer409WhenEmailIsTaken() throws Exception {
        when(createUserUseCase.execute(any()))
                .thenThrow(new ConflictException("user.emailAlreadyExists", "ana@workshop.com"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@workshop.com", "password": "Sup3r-Secret!",
                                 "roles": ["ADMIN"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "user.emailAlreadyExists",
                         "message": "Já existe um usuário com o e-mail ana@workshop.com.", "details": null}
                        """));
    }

    @Test
    void shouldListWithDefaultPagination() throws Exception {
        when(listUsersUseCase.execute(new ListUsersQuery(0, 20)))
                .thenReturn(new Page<>(List.of(ANA), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(ANA_JSON)));
    }

    @Test
    void shouldValidatePagination() throws Exception {
        mockMvc.perform(get("/admin/users").param("page", "-1").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "page", "message": "Deve ser maior ou igual a zero."},
                            {"field": "size", "message": "Deve ser no máximo 100."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listUsersUseCase);
    }

    @Test
    void shouldFindUser() throws Exception {
        when(findUserUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(get("/admin/users/1")).andExpect(status().isOk()).andExpect(json(ANA_JSON));
    }

    @Test
    void shouldAnswer404ForUnknownUser() throws Exception {
        when(findUserUseCase.execute(any())).thenThrow(new NotFoundException("user.notFound", "1234"));

        mockMvc.perform(get("/admin/users/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "user.notFound", "message": "Usuário 1234 não encontrado.", "details": null}
                        """));
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
