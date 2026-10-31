package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import java.time.LocalDate;

public record GetServiceOrderTimeMetricsQuery(LocalDate from, LocalDate to) {
}
