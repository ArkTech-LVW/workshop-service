package br.com.arktech.workshop.auth.application.login;

import br.com.arktech.workshop.auth.application.port.AccessToken;
import br.com.arktech.workshop.auth.application.port.LoginRateLimiter;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.exception.TooManyRequestsException;
import br.com.arktech.workshop.shared.domain.exception.UnauthorizedException;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
    private final RecordingLoginRateLimiter loginRateLimiter = new RecordingLoginRateLimiter();
    private final LoginUseCase loginUseCase =
            new LoginUseCase(userRepository, passwordHasher, tokenIssuer, loginRateLimiter, () -> "10.0.0.1");

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
    void shouldCountAttemptPerNormalizedEmailAndClientIp() {
        assertInvalidCredentials(new LoginCommand(" ANA@Workshop.com ", "wrong"));

        assertThat(loginRateLimiter.attempts).containsExactly("ana@workshop.com 10.0.0.1");
    }

    @Test
    void shouldRejectOverTheLimitBeforeCheckingThePassword() {
        userRepository.save(newUser());
        loginRateLimiter.overLimit = true;

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("ana@workshop.com", "secret")))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(passwordHasher.matchCount()).isZero();
        assertThat(tokenIssuer.roles).isNull();
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

    private static class RecordingLoginRateLimiter implements LoginRateLimiter {

        private final List<String> attempts = new ArrayList<>();
        private boolean overLimit;

        @Override
        public void consume(Email email, String clientIp) {
            if (overLimit) {
                throw new TooManyRequestsException(Duration.ofSeconds(12));
            }
            attempts.add(email.value() + " " + clientIp);
        }
    }
}
