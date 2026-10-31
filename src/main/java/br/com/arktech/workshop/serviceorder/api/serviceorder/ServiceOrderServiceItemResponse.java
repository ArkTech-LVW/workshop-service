package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

record ServiceOrderServiceItemResponse(
        @Schema(description = "${openapi.serviceOrderItem.itemId}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOrderItem.serviceOfferingId}", example = "1") Long serviceOfferingId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "1") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "150.00") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "150.00") BigDecimal subtotal,
        @Schema(description = "${openapi.serviceOrderItem.approvedAt}", example = "2026-10-28T10:15:00Z") @Nullable
                Instant approvedAt,
        @Schema(description = "${openapi.serviceOrderItem.completedAt}", example = "2026-10-28T16:45:00Z") @Nullable
                Instant completedAt) {

    static ServiceOrderServiceItemResponse from(ServiceItemResult result) {
        return new ServiceOrderServiceItemResponse(
                result.id(),
                result.serviceOfferingId(),
                result.quantity(),
                result.unitPrice(),
                result.subtotal(),
                result.approvedAt(),
                result.completedAt());
    }
}
