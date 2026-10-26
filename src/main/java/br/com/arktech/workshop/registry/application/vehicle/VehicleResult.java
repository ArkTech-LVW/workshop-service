package br.com.arktech.workshop.registry.application.vehicle;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;

import org.jspecify.annotations.Nullable;

public record VehicleResult(
        Long id,
        Long customerId,
        String licensePlate,
        @Nullable String chassis,
        String brand,
        String model,
        int year,
        String color) {

    public static VehicleResult from(Vehicle vehicle) {
        Chassis chassis = vehicle.getChassis();
        return new VehicleResult(
                vehicle.getId(),
                vehicle.getCustomerId(),
                vehicle.getLicensePlate().value(),
                chassis == null ? null : chassis.value(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor());
    }
}
