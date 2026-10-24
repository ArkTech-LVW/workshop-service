package br.com.arktech.workshop.shared.api.error;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record ApiError(String code, String message, @Nullable List<ApiFieldError> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
