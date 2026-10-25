package br.com.arktech.workshop.auth.application.port;

import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

public interface TokenIssuer {

    AccessToken issue(Long userId, Set<Role> roles);
}
