package br.com.arktech.workshop.shared.api.openapi;

import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.api.security.Authenticated;
import br.com.arktech.workshop.shared.api.security.PublicEndpoint;
import br.com.arktech.workshop.shared.domain.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Authenticated
class DocsTestController {

    @Operation(summary = "${openapi.response.internalError}")
    @GetMapping("/test/docs")
    DocsTestResponse docs() {
        return new DocsTestResponse("docs");
    }

    @PublicEndpoint
    @GetMapping("/test/docs/public")
    String publicDocs() {
        return "public";
    }

    @AllowedRoles(Role.ADMIN)
    @GetMapping("/test/docs/admin")
    String adminDocs() {
        return "admin";
    }

    record DocsTestResponse(@Schema(description = "${openapi.response.notFound}") String value) {}
}
