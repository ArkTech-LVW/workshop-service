package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;
import br.com.arktech.workshop.auth.infrastructure.config.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.application.port.AccessToken;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtTokenIssuerTest {

    private static final Duration EXPIRATION = Duration.ofHours(1);

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = properties("test-secret-with-at-least-thirty-two-bytes");
    private final JwtEncoder encoder = config.jwtEncoder(properties);
    private final JwtDecoder decoder = config.jwtDecoder(properties);
    private final JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(encoder, properties);

    @Test
    void shouldIssueTokenWithUserIdRolesAndExpiration() {
        AccessToken token = tokenIssuer.issue(42L, Set.of(Role.MECHANIC, Role.ADMIN));

        Jwt jwt = decoder.decode(token.value());

        assertThat(token.expiresIn()).isEqualTo(EXPIRATION);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "MECHANIC");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(EXPIRATION);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void shouldMapRolesToAuthorities() {
        Jwt jwt = decoder.decode(tokenIssuer.issue(42L, Set.of(Role.ADMIN, Role.ATTENDANT)).value());

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .filteredOn(authority -> authority.startsWith("ROLE_"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_ATTENDANT");
    }

    @Test
    void shouldRejectExpiredToken() {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        String token = encode(JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(EXPIRATION))
                .build());

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        String[] parts = tokenIssuer.issue(42L, Set.of(Role.MECHANIC)).value().split("\\.");
        String forgedPayload = new String(Base64.getUrlDecoder().decode(parts[1])).replace("MECHANIC", "ADMIN");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes())
                + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        JwtProperties otherProperties = properties("another-secret-with-at-least-thirty-two-bytes");
        String token = new JwtTokenIssuer(config.jwtEncoder(otherProperties), otherProperties)
                .issue(42L, Set.of(Role.ADMIN))
                .value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void shouldRefuseShortSecret() {
        assertThatThrownBy(() -> config.jwtEncoder(properties("short-secret")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static JwtProperties properties(String secret) {
        return new JwtProperties(secret, EXPIRATION);
    }
}
