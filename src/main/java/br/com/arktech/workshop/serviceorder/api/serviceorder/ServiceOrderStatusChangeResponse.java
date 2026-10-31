package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.StatusChangeResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

record ServiceOrderStatusChangeResponse(
        @Schema(description = "${openapi.serviceOrder.statusChange.fromStatus}", example = "RECEIVED") @Nullable
                ServiceOrderStatus fromStatus,
        @Schema(description = "${openapi.serviceOrder.statusChange.toStatus}", example = "IN_DIAGNOSIS")
                ServiceOrderStatus toStatus,
        @Schema(description = "${openapi.serviceOrder.statusChange.changedByUserId}", example = "1") @Nullable
                Long changedByUserId,
        @Schema(description = "${openapi.serviceOrder.statusChange.description}", example = "Pastilhas gastas")
                @Nullable String description,
        @Schema(description = "${openapi.serviceOrder.statusChange.changedAt}", example = "2026-10-27T15:00:00Z")
                @Nullable Instant changedAt) {

    static ServiceOrderStatusChangeResponse from(StatusChangeResult result) {
        return new ServiceOrderStatusChangeResponse(
                result.fromStatus(),
                result.toStatus(),
                result.changedByUserId(),
                result.description(),
                result.changedAt());
    }
}
