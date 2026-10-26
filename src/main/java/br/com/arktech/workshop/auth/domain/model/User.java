package br.com.arktech.workshop.auth.domain.model;

import br.com.arktech.workshop.auth.domain.port.PasswordHasher;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "\"user\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private String name;

    private Email email;

    @Getter(AccessLevel.NONE)
    private String passwordHash;

    private boolean active;

    @Getter(AccessLevel.NONE)
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> roles = new ArrayList<>();

    public User(String name, Email email, String password, Set<Role> roles, PasswordHasher passwordHasher) {
        requireRoles(roles);

        this.name = name;
        this.email = email;
        this.passwordHash = passwordHasher.hash(password);
        this.active = true;
        roles.forEach(role -> this.roles.add(new UserRole(this, role)));
    }

    public Set<Role> getRoles() {
        return roles.stream().map(UserRole::getRole).collect(Collectors.toUnmodifiableSet());
    }

    public boolean isActiveAdmin() {
        return active && getRoles().contains(Role.ADMIN);
    }

    public boolean authenticate(String password, PasswordHasher passwordHasher) {
        return passwordHasher.matches(password, passwordHash) && active;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeRoles(Set<Role> roles, long activeAdminCount) {
        requireRoles(roles);
        if (!roles.contains(Role.ADMIN)) {
            requireAnotherActiveAdmin(activeAdminCount);
        }

        this.roles.removeIf(userRole -> !roles.contains(userRole.getRole()));
        Set<Role> current = getRoles();
        roles.stream()
                .filter(role -> !current.contains(role))
                .forEach(role -> this.roles.add(new UserRole(this, role)));
    }

    public void activate() {
        active = true;
    }

    public void deactivate(long activeAdminCount) {
        requireAnotherActiveAdmin(activeAdminCount);
        active = false;
    }

    public void changePassword(String currentPassword, String newPassword, PasswordHasher passwordHasher) {
        if (!active) {
            throw new BusinessRuleException("user.inactive");
        }
        if (!passwordHasher.matches(currentPassword, passwordHash)) {
            throw new BusinessRuleException("user.wrongCurrentPassword");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BusinessRuleException("user.samePassword");
        }

        passwordHash = passwordHasher.hash(newPassword);
    }

    private void requireAnotherActiveAdmin(long activeAdminCount) {
        if (isActiveAdmin() && activeAdminCount <= 1) {
            throw new BusinessRuleException("user.lastActiveAdmin");
        }
    }

    private static void requireRoles(Set<Role> roles) {
        if (roles.isEmpty()) {
            throw new BusinessRuleException("user.roleRequired");
        }
    }
}
