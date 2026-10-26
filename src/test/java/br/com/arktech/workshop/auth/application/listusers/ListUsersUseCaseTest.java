package br.com.arktech.workshop.auth.application.listusers;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ListUsersUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final ListUsersUseCase listUsersUseCase = new ListUsersUseCase(userRepository);

    @Test
    void shouldListRequestedPageOrderedByName() {
        save("Caio");
        save("Ana");
        save("Bia");

        Page<UserResult> page = listUsersUseCase.execute(new ListUsersQuery(1, 2));

        assertThat(page.content()).extracting(UserResult::name).containsExactly("Caio");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    private void save(String name) {
        Email email = new Email(name.toLowerCase() + "@workshop.com");
        userRepository.save(new User(name, email, "secret", Set.of(Role.MECHANIC), new FakePasswordHasher()));
    }
}
