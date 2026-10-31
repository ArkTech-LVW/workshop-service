package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record CreateStockAdjustmentRequestRequest(
        @Schema(description = "${openapi.stockAdjustmentRequest.quantity}", example = "2") @NotNull @Positive
                Integer quantity,
        @Schema(description = "${openapi.stockAdjustmentRequest.reason}", example = "Avaria no transporte")
                @NotBlank @Size(max = 255, message = "{validation.maxLength}") String reason) {
}
