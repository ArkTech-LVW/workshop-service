package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;

import io.swagger.v3.oas.annotations.media.Schema;

import org.jspecify.annotations.Nullable;

record VehicleResponse(
        @Schema(description = "${openapi.vehicle.id}", example = "1") Long id,
        @Schema(description = "${openapi.vehicle.customerId}", example = "1") Long customerId,
        @Schema(description = "${openapi.vehicle.licensePlate}", example = "ABC1D23") String licensePlate,
        @Schema(description = "${openapi.vehicle.chassis}", example = "9BWZZZ372VT004251") @Nullable String chassis,
        @Schema(description = "${openapi.vehicle.brand}", example = "Volkswagen") String brand,
        @Schema(description = "${openapi.vehicle.model}", example = "Gol") String model,
        @Schema(description = "${openapi.vehicle.year}", example = "2020") int year,
        @Schema(description = "${openapi.vehicle.color}", example = "Prata") String color) {

    static VehicleResponse from(VehicleResult result) {
        return new VehicleResponse(
                result.id(),
                result.customerId(),
                result.licensePlate(),
                result.chassis(),
                result.brand(),
                result.model(),
                result.year(),
                result.color());
    }
}
