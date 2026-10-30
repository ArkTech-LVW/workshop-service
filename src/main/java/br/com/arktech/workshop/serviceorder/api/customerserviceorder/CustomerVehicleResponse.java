package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

import io.swagger.v3.oas.annotations.media.Schema;

record CustomerVehicleResponse(
        @Schema(description = "${openapi.vehicle.licensePlate}", example = "ABC1D23") String licensePlate,
        @Schema(description = "${openapi.vehicle.brand}", example = "Volkswagen") String brand,
        @Schema(description = "${openapi.vehicle.model}", example = "Gol") String model,
        @Schema(description = "${openapi.vehicle.year}", example = "2020") int year,
        @Schema(description = "${openapi.vehicle.color}", example = "Prata") String color) {

    static CustomerVehicleResponse from(VehicleDetails vehicle) {
        return new CustomerVehicleResponse(
                vehicle.licensePlate(), vehicle.brand(), vehicle.model(), vehicle.year(), vehicle.color());
    }
}
