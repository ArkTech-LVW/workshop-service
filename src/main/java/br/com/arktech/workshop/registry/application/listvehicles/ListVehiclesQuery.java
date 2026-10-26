package br.com.arktech.workshop.registry.application.listvehicles;

import org.jspecify.annotations.Nullable;

public record ListVehiclesQuery(@Nullable String licensePlate, @Nullable Long customerId, int page, int size) {
}
