package br.com.arktech.workshop.auth.application.changepassword;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements UseCase<ChangePasswordCommand, @Nullable Void> {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public @Nullable Void execute(ChangePasswordCommand command) {
        User user = userRepository.findRequiredById(currentUser.id());

        user.changePassword(command.currentPassword(), command.newPassword(), passwordHasher);
        userRepository.save(user);
        return null;
    }
}
