package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.jspecify.annotations.Nullable;

public record VehicleFilter(@Nullable LicensePlate licensePlate, @Nullable Long customerId) {
}
