package br.com.arktech.workshop.metrics.application.port;

import java.time.Duration;

public record StatusTime(String status, Duration averageTime, long serviceOrderCount) {
}
