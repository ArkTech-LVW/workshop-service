package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.auth.domain.port.PasswordHasher;

import java.util.Map;
import java.util.Objects;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class Argon2PasswordHasher implements PasswordHasher {

    private static final String ARGON2 = "argon2";

    private final PasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(
            ARGON2, Map.of(ARGON2, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));

    @Override
    public String hash(String password) {
        return Objects.requireNonNull(passwordEncoder.encode(password));
    }

    @Override
    public boolean matches(String password, String hash) {
        return passwordEncoder.matches(password, hash);
    }
}
