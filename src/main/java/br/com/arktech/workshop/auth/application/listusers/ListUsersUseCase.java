package br.com.arktech.workshop.auth.application.listusers;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase implements UseCase<ListUsersQuery, Page<UserResult>> {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResult> execute(ListUsersQuery query) {
        return userRepository.findPage(new Pagination(query.page(), query.size())).map(UserResult::from);
    }
}
