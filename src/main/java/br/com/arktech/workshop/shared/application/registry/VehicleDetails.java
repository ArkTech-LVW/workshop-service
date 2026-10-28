package br.com.arktech.workshop.shared.application.registry;

import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

public record VehicleDetails(LicensePlate licensePlate, String brand, String model, int year, String color) {
}
