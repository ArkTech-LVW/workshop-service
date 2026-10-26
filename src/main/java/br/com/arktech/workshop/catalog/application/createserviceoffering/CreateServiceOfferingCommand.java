package br.com.arktech.workshop.catalog.application.createserviceoffering;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CreateServiceOfferingCommand(
        String name, @Nullable String description, BigDecimal laborPrice, int estimatedMinutes) {
}
