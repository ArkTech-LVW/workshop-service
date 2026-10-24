package br.com.arktech.workshop.shared.api.error;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
class ApiErrorController implements ErrorController {

    private final ApiErrors apiErrors;

    @RequestMapping("${spring.web.error.path:${error.path:/error}}")
    ResponseEntity<ApiError> error(HttpServletRequest request) {
        Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        HttpStatusCode status = ex != null ? HttpStatus.INTERNAL_SERVER_ERROR : statusOf(request);
        return ApiErrors.response(status, apiErrors.forStatus(status, request.getMethod(), ex));
    }

    private static HttpStatusCode statusOf(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return statusCode instanceof Integer code ? HttpStatusCode.valueOf(code) : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
