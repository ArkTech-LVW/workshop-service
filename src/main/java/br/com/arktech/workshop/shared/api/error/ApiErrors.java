package br.com.arktech.workshop.shared.api.error;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ApiErrors {

    private static final String INVALID_FIELDS = "request.invalidFields";

    private final MessageResolver messageResolver;

    ApiError of(String code, Object... args) {
        return ApiError.of(code, messageResolver.get(code, args));
    }

    ApiError invalidField(String field, String messageCode) {
        return invalidFields(List.of(new ApiFieldError(field, messageResolver.get(messageCode))));
    }

    ApiError invalidFields(List<ApiFieldError> details) {
        return new ApiError(INVALID_FIELDS, messageResolver.get(INVALID_FIELDS), details);
    }

    ApiError forStatus(HttpStatusCode status, String method, Throwable ex) {
        if (status.is5xxServerError()) {
            return unexpected(ex);
        }
        return switch (status.value()) {
            case 404 -> of("request.routeNotFound");
            case 405 -> of("request.methodNotSupported", method);
            case 406 -> of("request.notAcceptable");
            case 415 -> of("request.unsupportedMediaType");
            default -> of("request.error");
        };
    }

    ApiError unexpected(Throwable ex) {
        String referenceCode = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unexpected error [referenceCode={}]", referenceCode, ex);
        return of("server.unexpectedError", referenceCode);
    }

    static ResponseEntity<ApiError> response(HttpStatusCode status, ApiError body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
