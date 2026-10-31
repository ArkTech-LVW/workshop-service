package br.com.arktech.workshop.inventory.application.stockadjustmentrequest;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record StockAdjustmentRequestResult(
        Long id,
        Long partId,
        int quantity,
        String reason,
        StockAdjustmentRequestStatus status,
        Long requestedByUserId,
        @Nullable Long reviewedByUserId,
        @Nullable String reviewNote,
        @Nullable Instant reviewedAt,
        @Nullable Instant dateCreated) {

    public static StockAdjustmentRequestResult from(StockAdjustmentRequest request) {
        return new StockAdjustmentRequestResult(
                request.getId(),
                request.getPart().getId(),
                request.getQuantity(),
                request.getReason(),
                request.getStatus(),
                request.getRequestedByUserId(),
                request.getReviewedByUserId(),
                request.getReviewNote(),
                request.getReviewedAt(),
                request.getDateCreated());
    }
}
