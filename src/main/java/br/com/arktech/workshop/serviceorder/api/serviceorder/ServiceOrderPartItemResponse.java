package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.PartItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

record ServiceOrderPartItemResponse(
        @Schema(description = "${openapi.serviceOrderItem.itemId}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOrderItem.partId}", example = "1") Long partId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "2") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "45.90") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "91.80") BigDecimal subtotal,
        @Schema(description = "${openapi.serviceOrderItem.approvedAt}", example = "2026-10-28T10:15:00Z") @Nullable
                Instant approvedAt) {

    static ServiceOrderPartItemResponse from(PartItemResult result) {
        return new ServiceOrderPartItemResponse(
                result.id(),
                result.partId(),
                result.quantity(),
                result.unitPrice(),
                result.subtotal(),
                result.approvedAt());
    }
}
