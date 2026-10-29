package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.PartItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

record ServiceOrderPartItemResponse(
        @Schema(description = "${openapi.serviceOrderItem.itemId}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOrderItem.partId}", example = "1") Long partId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "2") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "45.90") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "91.80") BigDecimal subtotal) {

    static ServiceOrderPartItemResponse from(PartItemResult result) {
        return new ServiceOrderPartItemResponse(
                result.id(), result.partId(), result.quantity(), result.unitPrice(), result.subtotal());
    }
}
