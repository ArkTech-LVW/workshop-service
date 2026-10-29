package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.shared.application.security.CurrentUser;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class JwtCurrentUser implements CurrentUser {

    @Override
    public Long id() {
        return findId().orElseThrow(() -> new IllegalStateException("No authenticated user in the current request"));
    }

    @Override
    public Optional<Long> findId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(token.getName()));
    }
}
