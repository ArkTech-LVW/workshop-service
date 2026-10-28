package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

record AddPartItemRequest(
        @Schema(description = "${openapi.serviceOrderItem.partId}", example = "1") @NotNull Long partId,
        @Schema(description = "${openapi.serviceOrderItem.quantity}", example = "2") @NotNull @Positive @Max(999)
                Integer quantity) {
}
