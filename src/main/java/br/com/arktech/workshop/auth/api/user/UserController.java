package br.com.arktech.workshop.auth.api.user;

import br.com.arktech.workshop.auth.application.createuser.CreateUserCommand;
import br.com.arktech.workshop.auth.application.createuser.CreateUserUseCase;
import br.com.arktech.workshop.auth.application.finduser.FindUserQuery;
import br.com.arktech.workshop.auth.application.finduser.FindUserUseCase;
import br.com.arktech.workshop.auth.application.listusers.ListUsersQuery;
import br.com.arktech.workshop.auth.application.listusers.ListUsersUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class UserController implements IUserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final FindUserUseCase findUserUseCase;

    @Override
    public UserResponse create(CreateUserRequest request) {
        CreateUserCommand command =
                new CreateUserCommand(request.name(), request.email(), request.password(), request.roles());
        return UserResponse.from(createUserUseCase.execute(command));
    }

    @Override
    public PageResponse<UserResponse> list(int page, int size) {
        return PageResponse.from(listUsersUseCase.execute(new ListUsersQuery(page, size)), UserResponse::from);
    }

    @Override
    public UserResponse findById(Long id) {
        return UserResponse.from(findUserUseCase.execute(new FindUserQuery(id)));
    }
}
