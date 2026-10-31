package br.com.arktech.workshop.metrics.application.port;

import java.time.Duration;

public record ServiceExecutionTime(
        Long serviceOfferingId,
        String name,
        int estimatedMinutes,
        Duration averageExecutionTime,
        long serviceOrderCount) {
}
