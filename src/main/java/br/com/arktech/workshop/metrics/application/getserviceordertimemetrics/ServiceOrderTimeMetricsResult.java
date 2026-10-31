package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;
import br.com.arktech.workshop.metrics.application.port.StatusTime;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record ServiceOrderTimeMetricsResult(
        long serviceOrderCount,
        @Nullable Duration averageExecutionTime,
        List<ServiceExecutionTime> byService,
        List<StatusTime> timeByStatus) {

    public ServiceOrderTimeMetricsResult {
        byService = List.copyOf(byService);
        timeByStatus = List.copyOf(timeByStatus);
    }
}
