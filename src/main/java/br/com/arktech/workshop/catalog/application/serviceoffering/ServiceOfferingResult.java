package br.com.arktech.workshop.catalog.application.serviceoffering;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record ServiceOfferingResult(
        Long id,
        String name,
        @Nullable String description,
        BigDecimal laborPrice,
        int estimatedMinutes,
        boolean active) {

    public static ServiceOfferingResult from(ServiceOffering serviceOffering) {
        return new ServiceOfferingResult(
                serviceOffering.getId(),
                serviceOffering.getName(),
                serviceOffering.getDescription(),
                serviceOffering.getLaborPrice().amount(),
                serviceOffering.getEstimatedMinutes(),
                serviceOffering.isActive());
    }
}
