package br.com.arktech.workshop.auth.api.authentication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.auth.application.login.LoginCommand;
import br.com.arktech.workshop.auth.application.login.LoginResult;
import br.com.arktech.workshop.auth.application.login.LoginUseCase;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.UnauthorizedException;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Test
    void shouldReturnBearerTokenWithExpirationInSeconds() throws Exception {
        when(loginUseCase.execute(any())).thenReturn(new LoginResult("jwt-token", Duration.ofHours(1)));

        login("""
                {"email": "ana@workshop.com", "password": "secret"}
                """)
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"accessToken": "jwt-token", "tokenType": "Bearer", "expiresIn": 3600}
                        """));
        verify(loginUseCase).execute(new LoginCommand("ana@workshop.com", "secret"));
    }

    @Test
    void shouldRequireEmailAndPassword() throws Exception {
        login("{}")
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {
                          "code": "request.invalidFields",
                          "message": "Existem campos inválidos na requisição.",
                          "details": [
                            {"field": "email", "message": "Não pode estar em branco."},
                            {"field": "password", "message": "Não pode estar em branco."}
                          ]
                        }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldRejectMalformedEmail() throws Exception {
        login("""
                {"email": "not-an-email", "password": "secret"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"details": [{"field": "email", "message": "E-mail inválido."}]}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldAnswer401WhenCredentialsAreInvalid() throws Exception {
        when(loginUseCase.execute(any())).thenThrow(new UnauthorizedException("auth.invalidCredentials"));

        login("""
                {"email": "ana@workshop.com", "password": "wrong"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(json("""
                        {"code": "auth.invalidCredentials", "message": "E-mail ou senha inválidos.", "details": null}
                        """));
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
