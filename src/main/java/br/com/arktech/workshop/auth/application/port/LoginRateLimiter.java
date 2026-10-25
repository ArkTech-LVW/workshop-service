package br.com.arktech.workshop.auth.application.port;

import br.com.arktech.workshop.shared.domain.email.Email;

public interface LoginRateLimiter {

    void consume(Email email, String clientIp);
}
