package br.com.arktech.workshop.auth.application.updateuser;

import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

public record UpdateUserCommand(Long id, String name, Set<Role> roles) {
}
