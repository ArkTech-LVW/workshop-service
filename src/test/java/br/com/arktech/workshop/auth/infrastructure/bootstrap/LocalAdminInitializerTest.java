package br.com.arktech.workshop.auth.infrastructure.bootstrap;

import br.com.arktech.workshop.auth.infrastructure.config.LocalAdminProperties;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.auth.application.createuser.CreateUserUseCase;
import br.com.arktech.workshop.auth.domain.port.FakePasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.InMemoryUserRepository;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.security.Role;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LocalAdminInitializerTest {

    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final LocalAdminInitializer initializer = new LocalAdminInitializer(
            new LocalAdminProperties("Administrador", "Admin@Workshop.local", "admin123"),
            userRepository,
            new CreateUserUseCase(userRepository, passwordHasher));

    @Test
    void shouldCreateAdminWhenMissing() {
        initializer.run(new DefaultApplicationArguments());

        User admin = userRepository.findByEmail("admin@workshop.local").orElseThrow();
        assertThat(admin.getRoles()).containsExactly(Role.ADMIN);
        assertThat(admin.authenticate("admin123", passwordHasher)).isTrue();
    }

    @Test
    void shouldKeepExistingAdminOnNextStartup() {
        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldHidePasswordFromToString() {
        assertThat(new LocalAdminProperties("Administrador", "admin@workshop.local", "admin123").toString())
                .doesNotContain("admin123");
    }
}
