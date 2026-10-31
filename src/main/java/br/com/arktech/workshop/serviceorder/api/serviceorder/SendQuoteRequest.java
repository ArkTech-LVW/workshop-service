package br.com.arktech.workshop.serviceorder.api.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;

import org.jspecify.annotations.Nullable;

public record SendQuoteRequest(
        @Schema(description = "${openapi.serviceOrder.canProceedWithoutChanges}", example = "true") @Nullable
                Boolean canProceedWithoutChanges) {
}
