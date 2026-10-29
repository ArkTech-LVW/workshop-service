package br.com.arktech.workshop.inventory.api.stockmovement;

import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

record StockMovementResponse(
        @Schema(description = "${openapi.stockMovement.id}", example = "1") Long id,
        @Schema(description = "${openapi.part.id}", example = "1") Long partId,
        @Schema(description = "${openapi.stockMovement.type}", example = "IN") StockMovementType type,
        @Schema(description = "${openapi.stockMovement.quantity}", example = "10") int quantity,
        @Schema(description = "${openapi.stockMovement.reason}", example = "Compra do fornecedor, NF 1234")
                @Nullable String reason,
        @Schema(description = "${openapi.stockMovement.serviceOrderId}", example = "null") @Nullable
                Long serviceOrderId,
        @Schema(description = "${openapi.stockMovement.createdByUserId}", example = "1") @Nullable
                Long createdByUserId,
        @Schema(description = "${openapi.stockMovement.dateCreated}", example = "2026-10-26T14:30:00Z") @Nullable
                Instant dateCreated) {

    static StockMovementResponse from(StockMovementResult result) {
        return new StockMovementResponse(
                result.id(),
                result.partId(),
                result.type(),
                result.quantity(),
                result.reason(),
                result.serviceOrderId(),
                result.createdByUserId(),
                result.dateCreated());
    }
}
