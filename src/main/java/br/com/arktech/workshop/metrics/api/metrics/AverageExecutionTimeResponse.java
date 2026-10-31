package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.ServiceOrderTimeMetricsResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import org.jspecify.annotations.Nullable;

record AverageExecutionTimeResponse(
        @Schema(description = "${openapi.metrics.serviceOrderCount}", example = "42") long serviceOrderCount,
        @Schema(description = "${openapi.metrics.averageExecutionTimeOverall}")
                @Nullable DurationResponse averageExecutionTime,
        @Schema(description = "${openapi.metrics.byService}") List<ServiceExecutionTimeResponse> byService,
        @Schema(description = "${openapi.metrics.timeByStatus}") List<StatusTimeResponse> timeByStatus) {

    static AverageExecutionTimeResponse from(ServiceOrderTimeMetricsResult result) {
        return new AverageExecutionTimeResponse(
                result.serviceOrderCount(),
                DurationResponse.fromNullable(result.averageExecutionTime()),
                result.byService().stream().map(ServiceExecutionTimeResponse::from).toList(),
                result.timeByStatus().stream().map(StatusTimeResponse::from).toList());
    }
}
