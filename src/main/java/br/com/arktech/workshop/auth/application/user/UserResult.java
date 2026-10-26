package br.com.arktech.workshop.auth.application.user;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record UserResult(Long id, String name, String email, boolean active, Set<Role> roles) {

    public UserResult {
        roles = Collections.unmodifiableSortedSet(new TreeSet<>(roles));
    }

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getName(), user.getEmail().value(), user.isActive(), user.getRoles());
    }
}
