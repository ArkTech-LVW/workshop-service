package br.com.arktech.workshop.shared.api.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Gives the generated spec an operation to check the global responses, security and text placeholders against
@RestController
class DocsTestController {

    // Reuses existing keys: the point is checking that ${...} resolves from openapi.properties
    @Operation(summary = "${openapi.response.internalError}")
    @GetMapping("/test/docs")
    DocsTestResponse docs() {
        return new DocsTestResponse("docs");
    }

    @SecurityRequirements
    @GetMapping("/test/docs/public")
    String publicDocs() {
        return "public";
    }

    record DocsTestResponse(@Schema(description = "${openapi.response.notFound}") String value) {}
}
