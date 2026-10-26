package br.com.arktech.workshop.auth.api.user;

import br.com.arktech.workshop.auth.application.activateuser.ActivateUserCommand;
import br.com.arktech.workshop.auth.application.activateuser.ActivateUserUseCase;
import br.com.arktech.workshop.auth.application.createuser.CreateUserCommand;
import br.com.arktech.workshop.auth.application.createuser.CreateUserUseCase;
import br.com.arktech.workshop.auth.application.deactivateuser.DeactivateUserCommand;
import br.com.arktech.workshop.auth.application.deactivateuser.DeactivateUserUseCase;
import br.com.arktech.workshop.auth.application.finduser.FindUserQuery;
import br.com.arktech.workshop.auth.application.finduser.FindUserUseCase;
import br.com.arktech.workshop.auth.application.listusers.ListUsersQuery;
import br.com.arktech.workshop.auth.application.listusers.ListUsersUseCase;
import br.com.arktech.workshop.auth.application.updateuser.UpdateUserCommand;
import br.com.arktech.workshop.auth.application.updateuser.UpdateUserUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class UserController implements IUserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;

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

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {
        return UserResponse.from(updateUserUseCase.execute(new UpdateUserCommand(id, request.name(), request.roles())));
    }

    @Override
    public UserResponse activate(Long id) {
        return UserResponse.from(activateUserUseCase.execute(new ActivateUserCommand(id)));
    }

    @Override
    public UserResponse deactivate(Long id) {
        return UserResponse.from(deactivateUserUseCase.execute(new DeactivateUserCommand(id)));
    }
}
