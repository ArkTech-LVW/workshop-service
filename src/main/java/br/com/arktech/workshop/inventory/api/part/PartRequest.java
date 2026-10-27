package br.com.arktech.workshop.inventory.api.part;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.shared.api.validation.Money;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record PartRequest(
        @Schema(description = "${openapi.part.sku}", example = "OLEO-5W30-1L")
                @NotNull @Pattern(regexp = PartRequest.SKU_PATTERN, message = "{sku.invalid}") String sku,
        @Schema(description = "${openapi.part.name}", example = "Óleo 5W30 sintético 1L")
                @NotBlank @Size(max = 150, message = "{validation.maxLength}") String name,
        @Schema(description = "${openapi.part.type}", example = "SUPPLY") @NotNull PartType type,
        @Schema(description = "${openapi.part.unit}", example = "LITER") @NotNull MeasurementUnit unit,
        @Schema(description = "${openapi.part.salePrice}", example = "45.90") @NotNull @Money BigDecimal salePrice,
        @Schema(description = "${openapi.part.minimumStock}", example = "10") @NotNull @PositiveOrZero
                Integer minimumStock) {

    static final String SKU_PATTERN = "\\s*[A-Za-z0-9._/-]{1,50}\\s*";
}
