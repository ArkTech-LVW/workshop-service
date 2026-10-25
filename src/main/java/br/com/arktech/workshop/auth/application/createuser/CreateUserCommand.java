package br.com.arktech.workshop.auth.application.createuser;

import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

public record CreateUserCommand(String name, String email, String password, Set<Role> roles) {

    @Override
    public String toString() {
        return "CreateUserCommand[name=" + name + ", email=" + email + ", password=****, roles=" + roles + "]";
    }
}
