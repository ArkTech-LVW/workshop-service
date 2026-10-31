package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import br.com.arktech.workshop.metrics.application.port.StatusTime;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record ServiceOrderTimeMetricsResult(
        long serviceOrderCount, @Nullable Duration averageExecutionTime, List<StatusTime> timeByStatus) {

    public ServiceOrderTimeMetricsResult {
        timeByStatus = List.copyOf(timeByStatus);
    }
}
