package br.com.arktech.workshop.auth.application.changepassword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangePasswordUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();

    private User user;
    private ChangePasswordUseCase changePasswordUseCase;

    @BeforeEach
    void setUp() {
        userRepository.save(
                new User("Bia", new Email("bia@workshop.com"), "other", Set.of(Role.MECHANIC), passwordHasher));
        user = userRepository.save(
                new User("Ana", new Email("ana@workshop.com"), "secret", Set.of(Role.MECHANIC), passwordHasher));
        changePasswordUseCase = new ChangePasswordUseCase(userRepository, passwordHasher, user::getId);
    }

    @Test
    void shouldChangePasswordOfTheCurrentUser() {
        changePasswordUseCase.execute(new ChangePasswordCommand("secret", "new-secret"));

        assertThat(user.authenticate("new-secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        assertThatThrownBy(() -> changePasswordUseCase.execute(new ChangePasswordCommand("other", "new-secret")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.wrongCurrentPassword");
        assertThat(user.authenticate("secret", passwordHasher)).isTrue();
    }

    @Test
    void shouldRejectInactiveUser() {
        user.deactivate(0);

        assertThatThrownBy(() -> changePasswordUseCase.execute(new ChangePasswordCommand("secret", "new-secret")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.inactive");
    }

    @Test
    void shouldRejectSamePassword() {
        assertThatThrownBy(() -> changePasswordUseCase.execute(new ChangePasswordCommand("secret", "secret")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.samePassword");
    }

    @Test
    void shouldHidePasswordsFromToString() {
        assertThat(new ChangePasswordCommand("secret", "new-secret").toString())
                .doesNotContain("secret");
    }
}
