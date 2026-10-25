package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import br.com.arktech.workshop.auth.application.port.LoginRateLimiter;
import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;
import br.com.arktech.workshop.shared.domain.email.Email;

import org.springframework.stereotype.Component;

@Component
class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private final RateLimit perEmailAndIp;

    InMemoryLoginRateLimiter(RateLimitProperties properties) {
        this.perEmailAndIp = new RateLimit(properties.loginPerEmailAndIp());
    }

    @Override
    public void consume(Email email, String clientIp) {
        perEmailAndIp.consume(email.value() + " " + clientIp);
    }
}
