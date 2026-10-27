package br.com.arktech.workshop.inventory.application.updatepart;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.PartType;

import java.math.BigDecimal;

public record UpdatePartCommand(
        Long id,
        String sku,
        String name,
        PartType type,
        MeasurementUnit unit,
        BigDecimal salePrice,
        int minimumStock) {
}
