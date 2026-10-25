package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.api.security.Authenticated;
import br.com.arktech.workshop.shared.api.security.PublicEndpoint;
import br.com.arktech.workshop.shared.domain.security.Role;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/test/security")
interface ISecurityTestController {

    @Authenticated
    @GetMapping("/me")
    Long me();

    @PublicEndpoint
    @GetMapping("/public")
    String publicEndpoint();

    @AllowedRoles({Role.ADMIN, Role.ATTENDANT})
    @GetMapping("/frontDesk")
    String frontDesk();

    // Deliberately without an access annotation: it must stay closed to everyone.
    @GetMapping("/undeclared")
    String undeclared();
}
