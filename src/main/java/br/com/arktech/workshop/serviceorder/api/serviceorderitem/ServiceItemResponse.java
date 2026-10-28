package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

record ServiceItemResponse(
        @Schema(description = "${openapi.serviceOrderItem.itemId}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOrderItem.serviceOfferingId}", example = "1") Long serviceOfferingId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "1") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "150.00") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "150.00") BigDecimal subtotal,
        @Schema(description = "${openapi.serviceOrderItem.serviceOrderTotalPrice}", example = "150.00")
                BigDecimal serviceOrderTotalPrice) {

    static ServiceItemResponse from(AddServiceItemResult result) {
        return new ServiceItemResponse(
                result.id(),
                result.serviceOfferingId(),
                result.quantity(),
                result.unitPrice(),
                result.subtotal(),
                result.serviceOrderTotalPrice());
    }
}
