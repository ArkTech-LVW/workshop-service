package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;

import io.swagger.v3.oas.annotations.media.Schema;

record ServiceExecutionTimeResponse(
        @Schema(description = "${openapi.metrics.serviceOfferingId}", example = "3") Long serviceOfferingId,
        @Schema(description = "${openapi.metrics.serviceName}", example = "Troca de óleo") String name,
        @Schema(description = "${openapi.metrics.estimatedMinutes}", example = "60") int estimatedMinutes,
        @Schema(description = "${openapi.metrics.serviceOrderCountByService}", example = "12") long serviceOrderCount,
        @Schema(description = "${openapi.metrics.executionCount}", example = "15") long executionCount,
        @Schema(description = "${openapi.metrics.averageExecutionTimeByService}")
                DurationResponse averageExecutionTime) {

    static ServiceExecutionTimeResponse from(ServiceExecutionTime service) {
        return new ServiceExecutionTimeResponse(
                service.serviceOfferingId(),
                service.name(),
                service.estimatedMinutes(),
                service.serviceOrderCount(),
                service.executionCount(),
                DurationResponse.from(service.averageExecutionTime()));
    }
}
