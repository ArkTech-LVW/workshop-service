package br.com.arktech.workshop;

import static br.com.arktech.workshop.UniqueTestData.uniqueEmail;

import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

@TestComponent
@RequiredArgsConstructor
public class TestUsers {

    private static final Long ANY_USER_ID = 1L;
    private static final String DEFAULT_PASSWORD = "secret";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public static RequestPostProcessor bearer(Role... roles) {
        return bearer(ANY_USER_ID, roles);
    }

    public static RequestPostProcessor bearer(Long userId, Role... roles) {
        return request -> {
            String token = bean(request, TokenIssuer.class).issue(userId, Set.of(roles)).value();
            return authorize(request, token);
        };
    }

    public static RequestPostProcessor bearer(String token) {
        return request -> authorize(request, token);
    }

    public static RequestPostProcessor expiredBearer() {
        return request -> {
            Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .subject(ANY_USER_ID.toString())
                    .issuedAt(issuedAt)
                    .expiresAt(issuedAt.plus(Duration.ofHours(1)))
                    .claim(JwtConfig.ROLES_CLAIM, List.of(Role.ADMIN.name()))
                    .build();
            JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
            String token = bean(request, JwtEncoder.class)
                    .encode(JwtEncoderParameters.from(header, claims))
                    .getTokenValue();
            return authorize(request, token);
        };
    }

    public User save(Role... roles) {
        return save(DEFAULT_PASSWORD, roles);
    }

    public User save(String password, Role... roles) {
        Email email = new Email(uniqueEmail());
        return userRepository.save(new User("Usuário de teste", email, password, Set.of(roles), passwordHasher));
    }

    private static <T> T bean(MockHttpServletRequest request, Class<T> type) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext())
                .getBean(type);
    }

    private static MockHttpServletRequest authorize(MockHttpServletRequest request, String token) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
