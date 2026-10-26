package br.com.arktech.workshop.shared.api.error;

import br.com.arktech.workshop.shared.api.security.PublicEndpoint;
import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;
import br.com.arktech.workshop.shared.api.validation.LicensePlate;
import br.com.arktech.workshop.shared.api.validation.Phone;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.exception.TooManyRequestsException;
import br.com.arktech.workshop.shared.domain.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PublicEndpoint
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

    @GetMapping("/unauthorized")
    void unauthorized() {
        throw new UnauthorizedException("test.unauthorized");
    }

    @GetMapping("/tooManyRequests")
    void tooManyRequests() {
        throw new TooManyRequestsException(Duration.ofMillis(11_500));
    }

    @GetMapping("/unauthenticated")
    void unauthenticated() {
        throw new InsufficientAuthenticationException("test");
    }

    @GetMapping("/accessDenied")
    void accessDenied() {
        throw new AccessDeniedException("test");
    }

    @GetMapping("/invalidStatusTransition")
    void invalidStatusTransition() {
        throw new InvalidStatusTransitionException("FINISHED");
    }

    @GetMapping("/missingDomainKey")
    void missingDomainKey() {
        throw new NotFoundException("test.missingKey");
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

    @PostMapping("/cpfCnpj")
    void cpfCnpj(@Valid @RequestBody CPFOrCNPJRequest request) {}

    @PostMapping("/licensePlate")
    void licensePlate(@Valid @RequestBody LicensePlateRequest request) {}

    @PostMapping("/phone")
    void phone(@Valid @RequestBody PhoneRequest request) {}

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

    record CPFOrCNPJRequest(@CPFOrCNPJ String document) {}

    record LicensePlateRequest(@LicensePlate String plate) {}

    record PhoneRequest(@Phone String phone) {}
}
