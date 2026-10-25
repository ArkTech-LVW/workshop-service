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

        user.deactivate();

        assertThat(user.isActive()).isFalse();
        assertThat(user.authenticate("secret", passwordHasher)).isFalse();
    }

    @Test
    void shouldCheckPasswordEvenWhenInactive() {
        User user = newUser();
        user.deactivate();

        user.authenticate("secret", passwordHasher);

        assertThat(passwordHasher.matchCount()).isEqualTo(1);
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
}
