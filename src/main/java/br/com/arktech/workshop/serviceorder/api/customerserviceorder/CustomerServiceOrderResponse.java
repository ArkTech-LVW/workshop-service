package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteResult;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

record CustomerServiceOrderResponse(
        @Schema(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                UUID publicId,
        @Schema(description = "${openapi.serviceOrder.number}", example = "SO-2026-000123") String number,
        @Schema(description = "${openapi.serviceOrder.status}", example = "AWAITING_APPROVAL")
                ServiceOrderStatus status,
        @Schema(description = "${openapi.customerServiceOrder.problemDescription}", example = "Barulho ao frear")
                String problemDescription,
        @Schema(description = "${openapi.serviceOrder.vehicle}") CustomerVehicleResponse vehicle,
        @Schema(description = "${openapi.customerServiceOrder.statusHistory}") List<StatusChangeResponse> statusHistory,
        @Schema(description = "${openapi.customerServiceOrder.quote}") @Nullable QuoteResponse quote,
        @Schema(description = "${openapi.serviceOrder.dateCreated}", example = "2026-10-27T14:30:00Z") @Nullable
                Instant dateCreated) {

    static CustomerServiceOrderResponse from(TrackServiceOrderResult result) {
        QuoteResult quote = result.quote();
        return new CustomerServiceOrderResponse(
                result.publicId(),
                result.number(),
                result.status(),
                result.problemDescription(),
                CustomerVehicleResponse.from(result.vehicle()),
                result.statusHistory().stream().map(StatusChangeResponse::from).toList(),
                quote == null ? null : QuoteResponse.from(quote),
                result.dateCreated());
    }
}
