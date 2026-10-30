package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

record QuoteResponse(
        @Schema(description = "${openapi.customerServiceOrder.quoteServices}") List<QuoteItemResponse> services,
        @Schema(description = "${openapi.customerServiceOrder.quoteParts}") List<QuoteItemResponse> parts,
        @Schema(description = "${openapi.serviceOrder.servicesTotal}", example = "150.00") BigDecimal servicesTotal,
        @Schema(description = "${openapi.serviceOrder.partsTotal}", example = "91.80") BigDecimal partsTotal,
        @Schema(description = "${openapi.customerServiceOrder.quoteTotalPrice}", example = "241.80")
                BigDecimal totalPrice) {

    static QuoteResponse from(QuoteResult result) {
        return new QuoteResponse(
                result.services().stream().map(QuoteItemResponse::from).toList(),
                result.parts().stream().map(QuoteItemResponse::from).toList(),
                result.servicesTotal(),
                result.partsTotal(),
                result.totalPrice());
    }
}
