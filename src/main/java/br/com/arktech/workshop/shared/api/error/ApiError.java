package br.com.arktech.workshop.shared.api.error;

import java.util.List;

public record ApiError(String code, String message, List<ApiFieldError> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
