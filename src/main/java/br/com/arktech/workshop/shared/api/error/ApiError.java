package br.com.arktech.workshop.shared.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "details"})
public record ApiError(String code, String message, @Nullable List<ApiFieldError> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
