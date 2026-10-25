package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class SecurityTestController implements ISecurityTestController {

    private final CurrentUser currentUser;

    @Override
    public Long me() {
        return currentUser.id();
    }
}
