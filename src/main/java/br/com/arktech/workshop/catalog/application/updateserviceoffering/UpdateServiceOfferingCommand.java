package br.com.arktech.workshop.catalog.application.updateserviceoffering;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record UpdateServiceOfferingCommand(
        Long id, String name, @Nullable String description, BigDecimal laborPrice, int estimatedMinutes) {
}
