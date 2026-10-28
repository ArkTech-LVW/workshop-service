package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

record PartItemResponse(
        @Schema(description = "${openapi.serviceOrderItem.itemId}", example = "2") Long id,
        @Schema(description = "${openapi.serviceOrderItem.partId}", example = "1") Long partId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "3") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "35.90") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "107.70") BigDecimal subtotal,
        @Schema(description = "${openapi.serviceOrderItem.serviceOrderTotalPrice}", example = "257.70")
                BigDecimal serviceOrderTotalPrice,
        @Schema(description = "${openapi.serviceOrderItem.availableQuantity}", example = "2") int availableQuantity,
        @Schema(description = "${openapi.serviceOrderItem.insufficientStock}", example = "true")
                boolean insufficientStock) {

    static PartItemResponse from(AddPartItemResult result) {
        return new PartItemResponse(
                result.id(),
                result.partId(),
                result.quantity(),
                result.unitPrice(),
                result.subtotal(),
                result.serviceOrderTotalPrice(),
                result.availableQuantity(),
                result.insufficientStock());
    }
}
