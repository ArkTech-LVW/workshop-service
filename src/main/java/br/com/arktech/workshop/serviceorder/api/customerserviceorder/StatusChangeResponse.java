package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.StatusChangeResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

record StatusChangeResponse(
        @Schema(description = "${openapi.customerServiceOrder.statusChangeStatus}", example = "AWAITING_APPROVAL")
                ServiceOrderStatus status,
        @Schema(description = "${openapi.customerServiceOrder.statusChangeDescription}", example = "null") @Nullable
                String description,
        @Schema(description = "${openapi.customerServiceOrder.statusChangeDate}", example = "2026-10-27T16:05:00Z")
                @Nullable Instant date) {

    static StatusChangeResponse from(StatusChangeResult result) {
        return new StatusChangeResponse(result.status(), result.description(), result.date());
    }
}
