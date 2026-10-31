package br.com.arktech.workshop.inventory.api.part;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.PartType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

record PartResponse(
        @Schema(description = "${openapi.part.id}", example = "1") Long id,
        @Schema(description = "${openapi.part.sku}", example = "OLEO-5W30-1L") String sku,
        @Schema(description = "${openapi.part.name}", example = "Óleo 5W30 sintético 1L") String name,
        @Schema(description = "${openapi.part.type}", example = "SUPPLY") PartType type,
        @Schema(description = "${openapi.part.unit}", example = "LITER") MeasurementUnit unit,
        @Schema(description = "${openapi.part.salePrice}", example = "45.90") BigDecimal salePrice,
        @Schema(description = "${openapi.part.stockQuantity}", example = "0") int stockQuantity,
        @Schema(description = "${openapi.part.reservedQuantity}", example = "0") int reservedQuantity,
        @Schema(description = "${openapi.part.availableQuantity}", example = "0") int availableQuantity,
        @Schema(description = "${openapi.part.minimumStock}", example = "10") int minimumStock) {

    static PartResponse from(PartResult result) {
        return new PartResponse(
                result.id(),
                result.sku(),
                result.name(),
                result.type(),
                result.unit(),
                result.salePrice(),
                result.stockQuantity(),
                result.reservedQuantity(),
                result.availableQuantity(),
                result.minimumStock());
    }
}
