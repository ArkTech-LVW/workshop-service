package br.com.arktech.workshop.auth.api.authentication;

import br.com.arktech.workshop.auth.application.changepassword.ChangePasswordCommand;
import br.com.arktech.workshop.auth.application.changepassword.ChangePasswordUseCase;
import br.com.arktech.workshop.auth.application.login.LoginCommand;
import br.com.arktech.workshop.auth.application.login.LoginUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController implements IAuthController {

    private final LoginUseCase loginUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @Override
    public LoginResponse login(LoginRequest request) {
        return LoginResponse.from(loginUseCase.execute(new LoginCommand(request.email(), request.password())));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        changePasswordUseCase.execute(new ChangePasswordCommand(request.currentPassword(), request.newPassword()));
    }
}
