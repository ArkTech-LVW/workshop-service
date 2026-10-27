package br.com.arktech.workshop.inventory.application.stockmovement;

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
        Long createdBy,
        @Nullable Instant dateCreated) {

    public static StockMovementResult from(StockMovement movement) {
        return new StockMovementResult(
                movement.getId(),
                movement.getPartId(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getServiceOrderId(),
                movement.getCreatedBy(),
                movement.getDateCreated());
    }
}
