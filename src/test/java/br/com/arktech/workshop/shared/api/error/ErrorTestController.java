package br.com.arktech.workshop.shared.api.error;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/errors")
class ErrorTestController {

    @GetMapping("/notFound")
    void notFound() {
        throw new NotFoundException("test.notFound", 42);
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new ConflictException("test.conflict");
    }

    @GetMapping("/businessRule")
    void businessRule() {
        throw new BusinessRuleException("test.businessRule");
    }

    @GetMapping("/invalidStatusTransition")
    void invalidStatusTransition() {
        throw new InvalidStatusTransitionException("FINISHED");
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("sensitive internal detail");
    }

    @GetMapping("/ok")
    OkResponse ok() {
        return new OkResponse("ok");
    }

    @PostMapping("/body")
    void body(@Valid @RequestBody TestRequest request) {}

    @GetMapping("/param")
    void param(@RequestParam @Positive Integer quantity) {}

    @GetMapping("/path/{id}")
    void path(@PathVariable Long id) {}

    @PostMapping("/missingKey")
    void missingKey(@Valid @RequestBody MissingKeyRequest request) {}

    @PostMapping("/literalMessage")
    void literalMessage(@Valid @RequestBody LiteralMessageRequest request) {}

    static class InvalidStatusTransitionException extends BusinessRuleException {

        InvalidStatusTransitionException(String status) {
            super("test.invalidStatusTransition", status);
        }
    }

    record OkResponse(String status) {}

    record TestRequest(@NotBlank @Size(min = 3, max = 10) String name, @NotNull @Valid List<TestItem> items) {}

    record TestItem(@NotBlank String name, @NotNull @Positive Integer quantity) {}

    record MissingKeyRequest(@NotBlank(message = "{test.missingKey}") String name) {}

    record LiteralMessageRequest(@NotBlank(message = "texto literal") String name) {}
}
