package br.com.arktech.workshop.auth.application.changepassword;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements VoidUseCase<ChangePasswordCommand> {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public void execute(ChangePasswordCommand command) {
        User user = userRepository.findRequiredById(currentUser.id());

        user.changePassword(command.currentPassword(), command.newPassword(), passwordHasher);
        userRepository.save(user);
    }
}
