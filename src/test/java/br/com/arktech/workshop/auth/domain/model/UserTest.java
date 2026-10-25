package br.com.arktech.workshop.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final Email EMAIL = new Email("ana@workshop.com");

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();

    @Test
    void shouldStartActiveWithEmailAndRoles() {
        User user = new User("Ana", EMAIL, "secret", Set.of(Role.ADMIN, Role.MECHANIC), passwordHasher);

        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.MECHANIC);
    }

    @Test
    void shouldRequireAtLeastOneRole() {
        assertThatThrownBy(() -> new User("Ana", EMAIL, "secret", Set.of(), passwordHasher))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.roleRequired");
    }

    @Test
    void shouldAuthenticateOnlyWithTheRightPassword() {
        User user = newUser();

        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
        assertThat(user.authenticate("wrong", passwordHasher)).isFalse();
    }

    @Test
    void shouldNotAuthenticateInactiveUser() {
        User user = newUser();

        user.deactivate(0);

        assertThat(user.isActive()).isFalse();
        assertThat(user.authenticate("secret", passwordHasher)).isFalse();
    }

    @Test
    void shouldCheckPasswordEvenWhenInactive() {
        User user = newUser();
        user.deactivate(0);

        user.authenticate("secret", passwordHasher);

        assertThat(passwordHasher.matchCount()).isEqualTo(1);
    }

    @Test
    void shouldReactivateUser() {
        User user = newUser();
        user.deactivate(0);

        user.activate();

        assertThat(user.isActive()).isTrue();
        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldRename() {
        User user = newUser();

        user.rename("Ana Souza");

        assertThat(user.getName()).isEqualTo("Ana Souza");
    }

    @Test
    void shouldReplaceRoles() {
        User user = new User("Ana", EMAIL, "secret", Set.of(Role.ATTENDANT, Role.MECHANIC), passwordHasher);

        user.changeRoles(Set.of(Role.MECHANIC, Role.ADMIN), 0);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.MECHANIC, Role.ADMIN);
    }

    @Test
    void shouldRequireAtLeastOneRoleWhenChangingRoles() {
        User user = newUser();

        assertThatThrownBy(() -> user.changeRoles(Set.of(), 2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.roleRequired");
        assertThat(user.getRoles()).containsExactly(Role.ATTENDANT);
    }

    @Test
    void shouldIdentifyActiveAdmin() {
        User admin = newAdmin();

        assertThat(admin.isActiveAdmin()).isTrue();
        assertThat(newUser().isActiveAdmin()).isFalse();

        admin.deactivate(2);

        assertThat(admin.isActiveAdmin()).isFalse();
    }

    @Test
    void shouldNotDeactivateTheLastActiveAdmin() {
        User admin = newAdmin();

        assertThatThrownBy(() -> admin.deactivate(1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.lastActiveAdmin");
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void shouldDeactivateAdminWhenAnotherActiveAdminExists() {
        User admin = newAdmin();

        admin.deactivate(2);

        assertThat(admin.isActive()).isFalse();
    }

    @Test
    void shouldNotRemoveAdminRoleFromTheLastActiveAdmin() {
        User admin = newAdmin();

        assertThatThrownBy(() -> admin.changeRoles(Set.of(Role.ATTENDANT), 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.lastActiveAdmin");
        assertThat(admin.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void shouldRemoveAdminRoleWhenAnotherActiveAdminExists() {
        User admin = newAdmin();

        admin.changeRoles(Set.of(Role.ATTENDANT), 2);

        assertThat(admin.getRoles()).containsExactly(Role.ATTENDANT);
    }

    @Test
    void shouldLetTheLastActiveAdminKeepAdminRoleWhileChangingOthers() {
        User admin = newAdmin();

        admin.changeRoles(Set.of(Role.ADMIN, Role.MECHANIC), 1);

        assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.MECHANIC);
    }

    @Test
    void shouldChangeInactiveAdminFreely() {
        User admin = newAdmin();
        admin.deactivate(2);

        admin.changeRoles(Set.of(Role.MECHANIC), 0);
        admin.deactivate(0);

        assertThat(admin.getRoles()).containsExactly(Role.MECHANIC);
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordMatches() {
        User user = newUser();

        user.changePassword("secret", "new-secret", passwordHasher);

        assertThat(user.authenticate("new-secret", passwordHasher)).isTrue();
        assertThat(user.authenticate("secret", passwordHasher)).isFalse();
    }

    @Test
    void shouldNotChangePasswordWhenCurrentPasswordIsWrong() {
        User user = newUser();

        assertThatThrownBy(() -> user.changePassword("wrong", "new-secret", passwordHasher))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.wrongCurrentPassword");
        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldNotChangePasswordToTheCurrentOne() {
        User user = newUser();

        assertThatThrownBy(() -> user.changePassword("secret", "secret", passwordHasher))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.samePassword");
    }

    @Test
    void shouldNotChangePasswordOfInactiveUser() {
        User user = newUser();
        user.deactivate(0);

        assertThatThrownBy(() -> user.changePassword("secret", "new-secret", passwordHasher))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.inactive");
        user.activate();
        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldExposeRolesAsReadOnly() {
        User user = newUser();

        assertThatThrownBy(() -> user.getRoles().add(Role.ADMIN)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldNotTreatUnsavedUsersAsEqual() {
        User user = newUser();

        assertThat(user).isEqualTo(user).isNotEqualTo(newUser());
    }

    private User newUser() {
        return new User("Ana", EMAIL, "secret", Set.of(Role.ATTENDANT), passwordHasher);
    }

    private User newAdmin() {
        return new User("Ana", EMAIL, "secret", Set.of(Role.ADMIN), passwordHasher);
    }
}
