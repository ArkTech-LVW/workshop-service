package br.com.arktech.workshop.shared.api.error;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.DomainException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INVALID_FIELDS = "request.invalidFields";
    private static final String INVALID_VALUE = "request.invalidValue";
    private static final String UNEXPECTED_ERROR = "server.unexpectedError";

    private final MessageResolver messageResolver;

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomainException(DomainException ex) {
        HttpStatus status = switch (ex) {
            case NotFoundException e -> HttpStatus.NOT_FOUND;
            case ConflictException e -> HttpStatus.CONFLICT;
            case BusinessRuleException e -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        return response(status, error(ex.getCode(), ex.getArgs()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, unexpectedError(ex));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiFieldError> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new ApiFieldError(
                        error instanceof FieldError fieldError ? fieldError.getField() : null,
                        error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(ex, invalidFields(details), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiFieldError> details = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameterName;
                details.add(new ApiFieldError(field, error.getDefaultMessage()));
            }
        });
        return handleExceptionInternal(ex, invalidFields(details), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, invalidField(ex.getPropertyName(), INVALID_VALUE), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ApiError body = invalidField(ex.getParameterName(), "request.requiredParameter");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiError body = ex.getCause() instanceof MismatchedInputException mismatch
                        && !mismatch.getPath().isEmpty()
                ? invalidField(path(mismatch.getPath()), INVALID_VALUE)
                : error("request.malformedBody");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ApiError apiError = body instanceof ApiError error ? error : errorFor(ex, statusCode);
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.putAll(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex, apiError, jsonHeaders, statusCode, request);
    }

    private ApiError errorFor(Exception ex, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return unexpectedError(ex);
        }
        return switch (status.value()) {
            case 404 -> error("request.routeNotFound");
            case 405 -> error(
                    "request.methodNotSupported",
                    ex instanceof HttpRequestMethodNotSupportedException e ? e.getMethod() : "");
            case 406 -> error("request.notAcceptable");
            case 415 -> error("request.unsupportedMediaType");
            default -> error("request.error");
        };
    }

    private ApiError unexpectedError(Exception ex) {
        String referenceCode = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unexpected error [referenceCode={}]", referenceCode, ex);
        return error(UNEXPECTED_ERROR, referenceCode);
    }

    private ApiError invalidField(String field, String messageCode) {
        return invalidFields(List.of(new ApiFieldError(field, messageResolver.get(messageCode))));
    }

    private ApiError invalidFields(List<ApiFieldError> details) {
        return new ApiError(INVALID_FIELDS, messageResolver.get(INVALID_FIELDS), details);
    }

    private ApiError error(String code, Object... args) {
        return ApiError.of(code, messageResolver.get(code, args));
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static String path(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            } else if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            }
        }
        return path.toString();
    }
}
