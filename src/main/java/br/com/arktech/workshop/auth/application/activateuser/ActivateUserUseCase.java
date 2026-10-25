package br.com.arktech.workshop.auth.application.activateuser;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivateUserUseCase implements UseCase<ActivateUserCommand, UserResult> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResult execute(ActivateUserCommand command) {
        User user = userRepository.findRequiredById(command.id());

        user.activate();
        return UserResult.from(userRepository.save(user));
    }
}
