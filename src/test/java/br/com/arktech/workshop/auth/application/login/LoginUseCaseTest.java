package br.com.arktech.workshop.auth.application.login;

import br.com.arktech.workshop.auth.application.port.AccessToken;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.exception.UnauthorizedException;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
    private final LoginUseCase loginUseCase = new LoginUseCase(userRepository, passwordHasher, tokenIssuer);

    @Test
    void shouldIssueTokenWithUserIdAndRoles() {
        User user = userRepository.save(
                new User("Ana", new Email("ana@workshop.com"), "secret", Set.of(Role.ADMIN, Role.MECHANIC), passwordHasher));

        LoginResult result = loginUseCase.execute(new LoginCommand("ana@workshop.com", "secret"));

        assertThat(result).isEqualTo(new LoginResult("token-" + user.getId(), Duration.ofHours(1)));
        assertThat(tokenIssuer.roles).containsExactlyInAnyOrder(Role.ADMIN, Role.MECHANIC);
    }

    @Test
    void shouldIgnoreCaseAndSpacesInEmail() {
        userRepository.save(newUser());

        LoginResult result = loginUseCase.execute(new LoginCommand("  ANA@Workshop.com ", "secret"));

        assertThat(result.accessToken()).startsWith("token-");
    }

    @Test
    void shouldRejectWrongPassword() {
        userRepository.save(newUser());

        assertInvalidCredentials(new LoginCommand("ana@workshop.com", "wrong"));
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = newUser();
        user.deactivate(0);
        userRepository.save(user);

        assertInvalidCredentials(new LoginCommand("ana@workshop.com", "secret"));
    }

    @Test
    void shouldSpendPasswordCheckTimeForUnknownEmail() {
        assertInvalidCredentials(new LoginCommand("nobody@workshop.com", "secret"));
        assertThat(passwordHasher.matchCount()).isEqualTo(1);
    }

    @Test
    void shouldHidePasswordFromToString() {
        assertThat(new LoginCommand("ana@workshop.com", "secret").toString()).doesNotContain("secret");
    }

    private void assertInvalidCredentials(LoginCommand command) {
        assertThatThrownBy(() -> loginUseCase.execute(command))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("auth.invalidCredentials");
        assertThat(tokenIssuer.roles).isNull();
    }

    private User newUser() {
        return new User("Ana", new Email("ana@workshop.com"), "secret", Set.of(Role.ATTENDANT), passwordHasher);
    }

    private static class RecordingTokenIssuer implements TokenIssuer {

        private Set<Role> roles;

        @Override
        public AccessToken issue(Long userId, Set<Role> roles) {
            this.roles = roles;
            return new AccessToken("token-" + userId, Duration.ofHours(1));
        }
    }
}
