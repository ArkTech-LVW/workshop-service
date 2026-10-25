package br.com.arktech.workshop.auth.application.deactivateuser;

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

class DeactivateUserUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final DeactivateUserUseCase deactivateUserUseCase = new DeactivateUserUseCase(userRepository);

    @Test
    void shouldDeactivateUser() {
        User user = save("ana@workshop.com", Role.MECHANIC);

        UserResult result = deactivateUserUseCase.execute(new DeactivateUserCommand(user.getId()));

        assertThat(result.active()).isFalse();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void shouldNotDeactivateTheLastActiveAdmin() {
        User admin = save("admin@workshop.com", Role.ADMIN);
        save("ana@workshop.com", Role.ATTENDANT);

        assertThatThrownBy(() -> deactivateUserUseCase.execute(new DeactivateUserCommand(admin.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.lastActiveAdmin");
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void shouldDeactivateAdminWhenAnotherActiveAdminExists() {
        User admin = save("admin@workshop.com", Role.ADMIN);
        save("other-admin@workshop.com", Role.ADMIN, Role.MECHANIC);

        deactivateUserUseCase.execute(new DeactivateUserCommand(admin.getId()));

        assertThat(admin.isActive()).isFalse();
    }

    @Test
    void shouldReportUnknownUser() {
        assertThatThrownBy(() -> deactivateUserUseCase.execute(new DeactivateUserCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("user.notFound");
    }

    private User save(String email, Role... roles) {
        return userRepository.save(new User("Ana", new Email(email), "secret", Set.of(roles), passwordHasher));
    }
}
