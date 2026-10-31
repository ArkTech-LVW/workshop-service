package br.com.arktech.workshop.inventory.application.part;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;

import java.math.BigDecimal;

public record PartResult(
        Long id,
        String sku,
        String name,
        PartType type,
        MeasurementUnit unit,
        BigDecimal salePrice,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity,
        int minimumStock) {

    public static PartResult from(Part part) {
        return new PartResult(
                part.getId(),
                part.getSku().value(),
                part.getName(),
                part.getType(),
                part.getUnit(),
                part.getSalePrice().amount(),
                part.getStockQuantity(),
                part.getReservedQuantity(),
                part.getAvailableQuantity(),
                part.getMinimumStock());
    }
}
