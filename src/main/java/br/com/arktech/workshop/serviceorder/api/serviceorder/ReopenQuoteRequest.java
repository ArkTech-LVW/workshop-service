package br.com.arktech.workshop.serviceorder.api.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

record ReopenQuoteRequest(
        @Schema(description = "${openapi.serviceOrder.reopenDescription}", example = "Cliente pediu peça mais barata")
                @Size(max = 2000, message = "{validation.maxLength}") @Nullable String description) {
}
