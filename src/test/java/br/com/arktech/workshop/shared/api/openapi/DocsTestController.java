package br.com.arktech.workshop.shared.api.openapi;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DocsTestController {

    @Operation(summary = "${openapi.response.internalError}")
    @GetMapping("/test/docs")
    String docs() {
        return "docs";
    }
}
