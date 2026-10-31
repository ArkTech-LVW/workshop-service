package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

record QuoteResponse(
        @Schema(description = "${openapi.customerServiceOrder.quoteServices}") List<QuoteItemResponse> services,
        @Schema(description = "${openapi.customerServiceOrder.quoteParts}") List<QuoteItemResponse> parts,
        @Schema(description = "${openapi.serviceOrder.servicesTotal}", example = "150.00") BigDecimal servicesTotal,
        @Schema(description = "${openapi.serviceOrder.partsTotal}", example = "91.80") BigDecimal partsTotal,
        @Schema(description = "${openapi.customerServiceOrder.quoteTotalPrice}", example = "241.80")
                BigDecimal totalPrice,
        @Schema(description = "${openapi.serviceOrder.approvedTotal}", example = "241.80") BigDecimal approvedTotal,
        @Schema(description = "${openapi.serviceOrder.pendingTotal}", example = "0.00") BigDecimal pendingTotal,
        @Schema(description = "${openapi.serviceOrder.canProceedWithoutChanges}", example = "null") @Nullable
                Boolean canProceedWithoutChanges) {

    static QuoteResponse from(QuoteResult result) {
        return new QuoteResponse(
                result.services().stream().map(QuoteItemResponse::from).toList(),
                result.parts().stream().map(QuoteItemResponse::from).toList(),
                result.servicesTotal(),
                result.partsTotal(),
                result.totalPrice(),
                result.approvedTotal(),
                result.pendingTotal(),
                result.canProceedWithoutChanges());
    }
}
