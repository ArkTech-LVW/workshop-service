package br.com.arktech.workshop.serviceorder.api.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CancelServiceOrderRequest(
        @Schema(description = "${openapi.serviceOrder.cancelReason}", example = "Cliente desistiu do serviço")
                @NotBlank @Size(max = 2000, message = "{validation.maxLength}") String reason) {
}
