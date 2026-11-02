package br.com.arktech.workshop.inventory.api.stockmovement;

import br.com.arktech.workshop.inventory.domain.model.StockMovementType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

record StockMovementRequest(
        @Schema(description = "${openapi.stockMovement.requestType}", example = "IN") @NotNull StockMovementType type,
        @Schema(description = "${openapi.stockMovement.quantity}", example = "10") @NotNull @Positive
                Integer quantity,
        @Schema(description = "${openapi.stockMovement.reason}", example = "NF 1234")
                @Size(max = 255, message = "{validation.maxLength}") @Nullable String reason) {
}
