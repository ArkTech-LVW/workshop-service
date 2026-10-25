package br.com.arktech.workshop.auth.infrastructure.bootstrap;

import br.com.arktech.workshop.auth.infrastructure.config.LocalAdminProperties;

import br.com.arktech.workshop.auth.application.createuser.CreateUserCommand;
import br.com.arktech.workshop.auth.application.createuser.CreateUserUseCase;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("development")
@EnableConfigurationProperties(LocalAdminProperties.class)
@RequiredArgsConstructor
class LocalAdminInitializer implements ApplicationRunner {

    private final LocalAdminProperties properties;
    private final UserRepository userRepository;
    private final CreateUserUseCase createUserUseCase;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(new Email(properties.email()))) {
            return;
        }

        createUserUseCase.execute(new CreateUserCommand(
                properties.name(), properties.email(), properties.password(), Set.of(Role.ADMIN)));
        log.info("Local admin {} created", properties.email());
    }
}
