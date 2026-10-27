package br.com.arktech.workshop.registry.application.createvehicle;

import org.jspecify.annotations.Nullable;

public record CreateVehicleCommand(
        Long customerId,
        String licensePlate,
        @Nullable String chassis,
        String brand,
        String model,
        int year,
        String color) {
}
