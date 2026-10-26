package br.com.arktech.workshop.auth.application.finduser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class FindUserUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final FindUserUseCase findUserUseCase = new FindUserUseCase(userRepository);

    @Test
    void shouldFindUserWithRolesInEnumOrder() {
        User user = userRepository.save(new User(
                "Ana",
                new Email("ana@workshop.com"),
                "secret",
                Set.of(Role.MECHANIC, Role.ADMIN),
                new FakePasswordHasher()));

        UserResult result = findUserUseCase.execute(new FindUserQuery(user.getId()));

        assertThat(result)
                .isEqualTo(new UserResult(user.getId(), "Ana", "ana@workshop.com", true, Set.of(Role.ADMIN, Role.MECHANIC)));
        assertThat(result.roles()).containsExactly(Role.ADMIN, Role.MECHANIC);
    }

    @Test
    void shouldReportUnknownUser() {
        assertThatThrownBy(() -> findUserUseCase.execute(new FindUserQuery(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("user.notFound")
                .extracting(error -> ((NotFoundException) error).getArgs())
                .isEqualTo(new Object[] {"1234"});
    }
}
