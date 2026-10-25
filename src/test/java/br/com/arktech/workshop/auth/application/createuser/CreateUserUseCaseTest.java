package br.com.arktech.workshop.auth.application.createuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CreateUserUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final CreateUserUseCase createUserUseCase = new CreateUserUseCase(userRepository, passwordHasher);

    @Test
    void shouldCreateUserWithHashedPassword() {
        UserResult result = createUserUseCase.execute(command("Ana@Workshop.com"));

        User user = userRepository.findByEmail(new Email("ana@workshop.com")).orElseThrow();
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(user.getRoles()).containsExactly(Role.ATTENDANT);
        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldRejectEmailAlreadyInUseIgnoringCase() {
        createUserUseCase.execute(command("ana@workshop.com"));

        assertThatThrownBy(() -> createUserUseCase.execute(command(" ANA@workshop.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("user.emailAlreadyExists");
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldHidePasswordFromToString() {
        assertThat(command("ana@workshop.com").toString()).doesNotContain("secret");
    }

    private static CreateUserCommand command(String email) {
        return new CreateUserCommand("Ana", email, "secret", Set.of(Role.ATTENDANT));
    }
}
