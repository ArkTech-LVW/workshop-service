package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RejectStockAdjustmentRequestRequest(
        @Schema(description = "${openapi.stockAdjustmentRequest.reviewNote}", example = "Peça encontrada no almoxarifado")
                @NotBlank @Size(max = 255, message = "{validation.maxLength}") String reviewNote) {
}
