package br.com.arktech.workshop.catalog.api.serviceoffering;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

record ServiceOfferingRequest(
        @Schema(description = "${openapi.serviceOffering.name}", example = "Troca de óleo")
                @NotBlank @Size(max = 150, message = "{validation.maxLength}") String name,
        @Schema(description = "${openapi.serviceOffering.description}", example = "Troca do óleo e do filtro do motor")
                @Size(max = 2000, message = "{validation.maxLength}") @Nullable String description,
        @Schema(description = "${openapi.serviceOffering.laborPrice}", example = "150.00")
                @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal laborPrice,
        @Schema(description = "${openapi.serviceOffering.estimatedMinutes}", example = "60")
                @NotNull @Positive Integer estimatedMinutes) {
}
