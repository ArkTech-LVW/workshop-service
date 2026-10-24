package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;
import br.com.arktech.workshop.auth.infrastructure.config.JwtProperties;

import br.com.arktech.workshop.auth.application.port.AccessToken;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Instant;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    @Override
    public AccessToken issue(Long userId, Set<Role> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .claim(JwtConfig.ROLES_CLAIM, roles.stream().map(Role::name).sorted().toList())
                .build();
        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, properties.expiration());
    }
}
