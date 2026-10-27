package br.com.arktech.workshop.inventory.application.registerstockmovement;

import br.com.arktech.workshop.inventory.domain.model.StockMovementType;

import org.jspecify.annotations.Nullable;

public record RegisterStockMovementCommand(
        Long partId, StockMovementType type, int quantity, @Nullable String reason) {
}
