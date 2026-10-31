package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteItemResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

record QuoteItemResponse(
        @Schema(description = "${openapi.customerServiceOrder.quoteItemName}", example = "Pastilha de freio")
                String name,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "2") int quantity,
        @Schema(description = "${openapi.serviceOrderItem.unitPrice}", example = "45.90") BigDecimal unitPrice,
        @Schema(description = "${openapi.serviceOrderItem.subtotal}", example = "91.80") BigDecimal subtotal) {

    static QuoteItemResponse from(QuoteItemResult result) {
        return new QuoteItemResponse(result.name(), result.quantity(), result.unitPrice(), result.subtotal());
    }
}
