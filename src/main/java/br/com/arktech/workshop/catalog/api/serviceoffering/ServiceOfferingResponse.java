package br.com.arktech.workshop.catalog.api.serviceoffering;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

record ServiceOfferingResponse(
        @Schema(description = "${openapi.serviceOffering.id}", example = "1") Long id,
        @Schema(description = "${openapi.serviceOffering.name}", example = "Troca de óleo") String name,
        @Schema(description = "${openapi.serviceOffering.description}", example = "Troca do óleo e do filtro do motor")
                @Nullable String description,
        @Schema(description = "${openapi.serviceOffering.laborPrice}", example = "150.00") BigDecimal laborPrice,
        @Schema(description = "${openapi.serviceOffering.estimatedMinutes}", example = "60") int estimatedMinutes,
        @Schema(description = "${openapi.serviceOffering.active}", example = "true") boolean active) {

    static ServiceOfferingResponse from(ServiceOfferingResult result) {
        return new ServiceOfferingResponse(
                result.id(),
                result.name(),
                result.description(),
                result.laborPrice(),
                result.estimatedMinutes(),
                result.active());
    }
}
