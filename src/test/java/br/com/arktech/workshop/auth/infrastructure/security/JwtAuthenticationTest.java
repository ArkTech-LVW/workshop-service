package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@IntegrationTest
class JwtAuthenticationTest {

    private static final String UNAUTHENTICATED = """
            {
              "code": "auth.unauthenticated",
              "message": "É preciso se autenticar para acessar este recurso.",
              "details": null
            }
            """;
    private static final String INVALID_TOKEN = """
            {"code": "auth.invalidToken", "message": "Token de acesso inválido ou expirado.", "details": null}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void shouldAnswer401WithApiErrorWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/test/security/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(json(UNAUTHENTICATED));
    }

    @Test
    void shouldExposeTokenSubjectAsCurrentUser() throws Exception {
        String token = tokenIssuer.issue(42L, Set.of(Role.MECHANIC)).value();

        mockMvc.perform(get("/test/security/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(1)))
                .claim("roles", List.of("ADMIN"))
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(JwtConfig.ALGORITHM).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/test/security/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(json(INVALID_TOKEN));
    }

    @Test
    void shouldRejectTamperedToken() throws Exception {
        String token = tokenIssuer.issue(1L, Set.of(Role.ADMIN)).value();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/test/security/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(json(INVALID_TOKEN));
    }

    @Test
    void shouldRequireTokenOnUnknownRoute() throws Exception {
        mockMvc.perform(get("/test/security/doesNotExist")).andExpect(status().isUnauthorized());
    }

    private static ResultMatcher json(String expected) {
        return result -> {
            content().contentType(MediaType.APPLICATION_JSON).match(result);
            content().json(expected, JsonCompareMode.STRICT).match(result);
        };
    }
}
