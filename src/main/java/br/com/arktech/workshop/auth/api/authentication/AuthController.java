package br.com.arktech.workshop.auth.api.authentication;

import br.com.arktech.workshop.auth.application.login.LoginCommand;
import br.com.arktech.workshop.auth.application.login.LoginUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController implements IAuthController {

    private final LoginUseCase loginUseCase;

    @Override
    public LoginResponse login(LoginRequest request) {
        return LoginResponse.from(loginUseCase.execute(new LoginCommand(request.email(), request.password())));
    }
}
