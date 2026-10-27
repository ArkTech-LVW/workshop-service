package br.com.arktech.workshop.auth.api.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class LoginIntegrationTest {

    private static final String PASSWORD = "Sup3r-Secret!";
    private static final String INVALID_CREDENTIALS = """
            {"code": "auth.invalidCredentials", "message": "E-mail ou senha inválidos."}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void shouldIssueTokenAcceptedByProtectedEndpoints() throws Exception {
        User user = saveUser(true);

        String body = login(user.getEmail().value(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/test/security/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(user.getId().toString()));
    }

    @Test
    void shouldLoginEvenWithExpiredTokenInAuthorizationHeader() throws Exception {
        User user = saveUser(true);
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(user.getEmail().value(), PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldAnswerInvalidCredentialsEvenWithExpiredTokenInAuthorizationHeader() throws Exception {
        String body = """
                {"email": "nobody-%s@workshop.com", "password": "%s"}
                """.formatted(UUID.randomUUID(), PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(invalidCredentials());
    }

    @Test
    void shouldIgnoreEmailCase() throws Exception {
        User user = saveUser(true);

        login(user.getEmail().value().toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void shouldGiveTheSameAnswerForWrongPasswordAndUnknownEmail() throws Exception {
        User user = saveUser(true);

        login(user.getEmail().value(), "wrong-password").andExpect(status().isUnauthorized()).andExpect(invalidCredentials());
        login("nobody-" + UUID.randomUUID() + "@workshop.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(invalidCredentials());
    }

    @Test
    void shouldRejectInactiveUser() throws Exception {
        User user = saveUser(false);

        login(user.getEmail().value(), PASSWORD).andExpect(status().isUnauthorized()).andExpect(invalidCredentials());
    }

    @Test
    void shouldNeverExposePasswordInResponsesOrLogs(CapturedOutput output) throws Exception {
        User user = saveUser(true);

        String success = login(user.getEmail().value(), PASSWORD).andReturn().getResponse().getContentAsString();
        String failure = login(user.getEmail().value(), PASSWORD + "-wrong").andReturn().getResponse().getContentAsString();

        assertThat(success + failure).doesNotContain(PASSWORD).doesNotContain("argon2");
        assertThat(output.getAll()).doesNotContain(PASSWORD);
    }

    private User saveUser(boolean active) {
        String email = "user-" + UUID.randomUUID() + "@workshop.com";
        User user = new User("Ana", new Email(email), PASSWORD, Set.of(Role.ATTENDANT), passwordHasher);
        if (!active) {
            user.deactivate(0);
        }

        return userRepository.save(user);
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String expiredToken() {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(1)))
                .claim("roles", List.of("ADMIN"))
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(JwtConfig.ALGORITHM).build(), claims))
                .getTokenValue();
    }

    private static ResultMatcher invalidCredentials() {
        return content().json(INVALID_CREDENTIALS, JsonCompareMode.STRICT);
    }
}
