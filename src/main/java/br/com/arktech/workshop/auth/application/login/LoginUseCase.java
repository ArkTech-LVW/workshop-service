package br.com.arktech.workshop.auth.application.login;

import br.com.arktech.workshop.auth.application.port.AccessToken;
import br.com.arktech.workshop.auth.application.port.LoginRateLimiter;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;

import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.ClientAddress;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.UnauthorizedException;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LoginUseCase implements UseCase<LoginCommand, LoginResult> {

    private static final String INVALID_CREDENTIALS = "auth.invalidCredentials";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final LoginRateLimiter loginRateLimiter;
    private final ClientAddress clientAddress;
    private final String dummyHash;

    public LoginUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            LoginRateLimiter loginRateLimiter,
            ClientAddress clientAddress) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.loginRateLimiter = loginRateLimiter;
        this.clientAddress = clientAddress;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public LoginResult execute(LoginCommand command) {
        Email email = new Email(command.email());
        loginRateLimiter.consume(email, clientAddress.ip());

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            spendPasswordCheckTime(command.password());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (!user.authenticate(command.password(), passwordHasher)) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        AccessToken token = tokenIssuer.issue(user.getId(), user.getRoles());
        return new LoginResult(token.value(), token.expiresIn());
    }

    private void spendPasswordCheckTime(String password) {
        passwordHasher.matches(password, dummyHash);
    }
}
