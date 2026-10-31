package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

record ServiceOrderResponse(
        @Schema(description = "${openapi.serviceOrder.id}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                UUID publicId,
        @Schema(description = "${openapi.serviceOrder.number}", example = "SO-2026-000123") String number,
        @Schema(description = "${openapi.serviceOrder.customerId}", example = "1") Long customerId,
        @Schema(description = "${openapi.serviceOrder.vehicleId}", example = "1") Long vehicleId,
        @Schema(description = "${openapi.serviceOrder.mechanicId}", example = "null") @Nullable Long mechanicId,
        @Schema(description = "${openapi.serviceOrder.problemDescription}", example = "Barulho ao frear")
                String problemDescription,
        @Schema(description = "${openapi.serviceOrder.status}", example = "RECEIVED") ServiceOrderStatus status,
        @Schema(description = "${openapi.serviceOrder.services}") List<ServiceOrderServiceItemResponse> services,
        @Schema(description = "${openapi.serviceOrder.parts}") List<ServiceOrderPartItemResponse> parts,
        @Schema(description = "${openapi.serviceOrder.servicesTotal}", example = "0.00") BigDecimal servicesTotal,
        @Schema(description = "${openapi.serviceOrder.partsTotal}", example = "0.00") BigDecimal partsTotal,
        @Schema(description = "${openapi.serviceOrder.totalPrice}", example = "0.00") BigDecimal totalPrice,
        @Schema(description = "${openapi.serviceOrder.approvedTotal}", example = "0.00") BigDecimal approvedTotal,
        @Schema(description = "${openapi.serviceOrder.pendingTotal}", example = "0.00") BigDecimal pendingTotal,
        @Schema(description = "${openapi.serviceOrder.canProceedWithoutChanges}", example = "null") @Nullable
                Boolean canProceedWithoutChanges,
        @Schema(description = "${openapi.serviceOrder.statusHistory}")
                List<ServiceOrderStatusChangeResponse> statusHistory,
        @Schema(description = "${openapi.serviceOrder.dateCreated}", example = "2026-10-27T14:30:00Z") @Nullable
                Instant dateCreated) {

    static ServiceOrderResponse from(ServiceOrderResult result) {
        return new ServiceOrderResponse(
                result.id(),
                result.publicId(),
                result.number(),
                result.customerId(),
                result.vehicleId(),
                result.mechanicId(),
                result.problemDescription(),
                result.status(),
                result.services().stream()
                        .map(ServiceOrderServiceItemResponse::from)
                        .toList(),
                result.parts().stream().map(ServiceOrderPartItemResponse::from).toList(),
                result.servicesTotal(),
                result.partsTotal(),
                result.totalPrice(),
                result.approvedTotal(),
                result.pendingTotal(),
                result.canProceedWithoutChanges(),
                result.statusHistory().stream()
                        .map(ServiceOrderStatusChangeResponse::from)
                        .toList(),
                result.dateCreated());
    }
}
