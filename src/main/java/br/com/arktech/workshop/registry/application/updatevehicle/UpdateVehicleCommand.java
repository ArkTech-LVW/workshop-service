package br.com.arktech.workshop.registry.application.updatevehicle;

import org.jspecify.annotations.Nullable;

public record UpdateVehicleCommand(Long id, String licensePlate, @Nullable String chassis, String color) {
}
