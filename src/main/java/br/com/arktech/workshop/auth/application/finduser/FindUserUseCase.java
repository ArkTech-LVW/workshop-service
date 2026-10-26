package br.com.arktech.workshop.auth.application.finduser;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindUserUseCase implements UseCase<FindUserQuery, UserResult> {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResult execute(FindUserQuery query) {
        return UserResult.from(userRepository.findRequiredById(query.id()));
    }
}
