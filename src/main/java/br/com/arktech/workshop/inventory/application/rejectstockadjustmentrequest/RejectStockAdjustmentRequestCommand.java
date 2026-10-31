package br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest;

import org.jspecify.annotations.Nullable;

public record RejectStockAdjustmentRequestCommand(Long requestId, @Nullable String reviewNote) {
}
