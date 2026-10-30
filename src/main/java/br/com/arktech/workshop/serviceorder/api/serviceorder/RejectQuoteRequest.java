package br.com.arktech.workshop.serviceorder.api.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectQuoteRequest(
        @Schema(description = "${openapi.serviceOrder.rejectionReason}", example = "Cliente achou o valor alto")
                @NotBlank @Size(max = 2000, message = "{validation.maxLength}") String reason) {
}
