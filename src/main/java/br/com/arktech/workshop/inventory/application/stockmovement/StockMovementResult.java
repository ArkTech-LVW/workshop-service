package br.com.arktech.workshop.inventory.application.stockmovement;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record StockMovementResult(
        Long id,
        Long partId,
        StockMovementType type,
        int quantity,
        @Nullable String reason,
        @Nullable Long serviceOrderId,
        @Nullable Long createdByUserId,
        @Nullable Long stockAdjustmentRequestId,
        @Nullable Long requestedByUserId,
        @Nullable Instant dateCreated) {

    public static StockMovementResult from(StockMovement movement) {
        StockAdjustmentRequest request = movement.getStockAdjustmentRequest();
        return new StockMovementResult(
                movement.getId(),
                movement.getPart().getId(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getServiceOrderId(),
                movement.getCreatedByUserId(),
                request == null ? null : request.getId(),
                request == null ? null : request.getRequestedByUserId(),
                movement.getDateCreated());
    }
}
