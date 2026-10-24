package br.com.arktech.workshop.auth.domain.model;

import br.com.arktech.workshop.auth.domain.port.PasswordHasher;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "\"user\"")
@Getter
public class User extends BaseEntity {

    private String name;

    private String email;

    @Getter(AccessLevel.NONE)
    private String passwordHash;

    private boolean active;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> roles = new ArrayList<>();

    protected User() {
    }

    public User(String name, String email, String password, Set<Role> roles, PasswordHasher passwordHasher) {
        if (roles.isEmpty()) {
            throw new BusinessRuleException("user.roleRequired");
        }

        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHasher.hash(password);
        this.active = true;
        roles.forEach(role -> this.roles.add(new UserRole(this, role)));
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public Set<Role> getRoles() {
        return roles.stream().map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());
    }

    public boolean authenticate(String password, PasswordHasher passwordHasher) {
        return passwordHasher.matches(password, passwordHash) && active;
    }

    public void deactivate() {
        active = false;
    }
}
