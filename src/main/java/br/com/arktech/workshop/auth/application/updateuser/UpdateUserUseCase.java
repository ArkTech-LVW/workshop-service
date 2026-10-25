package br.com.arktech.workshop.auth.application.updateuser;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCase implements UseCase<UpdateUserCommand, UserResult> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResult execute(UpdateUserCommand command) {
        long activeAdminCount = userRepository.countActiveAdminsForUpdate();
        User user = userRepository.findRequiredById(command.id());

        user.rename(command.name());
        user.changeRoles(command.roles(), activeAdminCount);
        return UserResult.from(userRepository.save(user));
    }
}
