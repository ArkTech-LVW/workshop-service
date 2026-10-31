package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

record StockAdjustmentRequestResponse(
        @Schema(description = "${openapi.stockAdjustmentRequest.id}", example = "1") Long id,
        @Schema(description = "${openapi.part.id}", example = "1") Long partId,
        @Schema(description = "${openapi.stockAdjustmentRequest.quantity}", example = "2") int quantity,
        @Schema(description = "${openapi.stockAdjustmentRequest.reason}", example = "Avaria no transporte")
                String reason,
        @Schema(description = "${openapi.stockAdjustmentRequest.status}", example = "PENDING")
                StockAdjustmentRequestStatus status,
        @Schema(description = "${openapi.stockAdjustmentRequest.requestedByUserId}", example = "2")
                Long requestedByUserId,
        @Schema(description = "${openapi.stockAdjustmentRequest.reviewedByUserId}", example = "1") @Nullable
                Long reviewedByUserId,
        @Schema(description = "${openapi.stockAdjustmentRequest.reviewNote}", example = "null") @Nullable
                String reviewNote,
        @Schema(description = "${openapi.stockAdjustmentRequest.reviewedAt}", example = "2026-10-31T10:00:00Z")
                @Nullable Instant reviewedAt,
        @Schema(description = "${openapi.stockAdjustmentRequest.dateCreated}", example = "2026-10-30T14:30:00Z")
                @Nullable Instant dateCreated) {

    static StockAdjustmentRequestResponse from(StockAdjustmentRequestResult result) {
        return new StockAdjustmentRequestResponse(
                result.id(),
                result.partId(),
                result.quantity(),
                result.reason(),
                result.status(),
                result.requestedByUserId(),
                result.reviewedByUserId(),
                result.reviewNote(),
                result.reviewedAt(),
                result.dateCreated());
    }
}
