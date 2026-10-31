package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.metrics.application.port.StatusTime;

import io.swagger.v3.oas.annotations.media.Schema;

record StatusTimeResponse(
        @Schema(description = "${openapi.metrics.status}", example = "AWAITING_APPROVAL") String status,
        @Schema(description = "${openapi.metrics.serviceOrderCountByStatus}", example = "40") long serviceOrderCount,
        @Schema(description = "${openapi.metrics.averageTimeInStatus}") DurationResponse averageTime) {

    static StatusTimeResponse from(StatusTime statusTime) {
        return new StatusTimeResponse(
                statusTime.status(),
                statusTime.serviceOrderCount(),
                DurationResponse.from(statusTime.averageTime()));
    }
}
