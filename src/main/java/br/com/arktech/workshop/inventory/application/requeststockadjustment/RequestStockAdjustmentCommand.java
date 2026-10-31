package br.com.arktech.workshop.inventory.application.requeststockadjustment;

import org.jspecify.annotations.Nullable;

public record RequestStockAdjustmentCommand(Long partId, int quantity, @Nullable String reason) {
}
