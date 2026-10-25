package br.com.arktech.workshop.auth.application.updateuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UpdateUserUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final UpdateUserUseCase updateUserUseCase = new UpdateUserUseCase(userRepository);

    @Test
    void shouldUpdateNameAndRoles() {
        User user = save("ana@workshop.com", Role.ATTENDANT);

        UserResult result =
                updateUserUseCase.execute(new UpdateUserCommand(user.getId(), "Ana Souza", Set.of(Role.MECHANIC)));

        assertThat(result.name()).isEqualTo("Ana Souza");
        assertThat(result.roles()).containsExactly(Role.MECHANIC);
        assertThat(user.getRoles()).containsExactly(Role.MECHANIC);
    }

    @Test
    void shouldNotRemoveAdminRoleFromTheLastActiveAdmin() {
        User admin = save("admin@workshop.com", Role.ADMIN);
        User inactiveAdmin = save("old-admin@workshop.com", Role.ADMIN);
        inactiveAdmin.deactivate(2);

        assertThatThrownBy(() -> updateUserUseCase.execute(
                        new UpdateUserCommand(admin.getId(), "Ana", Set.of(Role.ATTENDANT))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.lastActiveAdmin");
        assertThat(admin.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void shouldRemoveAdminRoleWhenAnotherActiveAdminExists() {
        User admin = save("admin@workshop.com", Role.ADMIN);
        save("other-admin@workshop.com", Role.ADMIN);

        updateUserUseCase.execute(new UpdateUserCommand(admin.getId(), "Ana", Set.of(Role.ATTENDANT)));

        assertThat(admin.getRoles()).containsExactly(Role.ATTENDANT);
    }

    @Test
    void shouldReportUnknownUser() {
        assertThatThrownBy(() -> updateUserUseCase.execute(new UpdateUserCommand(99L, "Ana", Set.of(Role.ADMIN))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("user.notFound");
    }

    private User save(String email, Role... roles) {
        return userRepository.save(new User("Ana", new Email(email), "secret", Set.of(roles), passwordHasher));
    }
}
