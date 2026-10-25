package br.com.arktech.workshop.auth.application.activateuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ActivateUserUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final ActivateUserUseCase activateUserUseCase = new ActivateUserUseCase(userRepository);

    @Test
    void shouldActivateUser() {
        User user = userRepository.save(
                new User("Ana", new Email("ana@workshop.com"), "secret", Set.of(Role.MECHANIC), passwordHasher));
        user.deactivate(0);

        UserResult result = activateUserUseCase.execute(new ActivateUserCommand(user.getId()));

        assertThat(result.active()).isTrue();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void shouldReportUnknownUser() {
        assertThatThrownBy(() -> activateUserUseCase.execute(new ActivateUserCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("user.notFound");
    }
}
