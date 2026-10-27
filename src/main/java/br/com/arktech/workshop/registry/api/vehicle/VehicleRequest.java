package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.shared.api.validation.Chassis;
import br.com.arktech.workshop.shared.api.validation.LicensePlate;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

record VehicleRequest(
        @Schema(description = "${openapi.vehicle.customerId}", example = "1") @NotNull Long customerId,
        @Schema(description = "${openapi.vehicle.licensePlate}", example = "ABC-1D23")
                @NotBlank @LicensePlate String licensePlate,
        @Schema(description = "${openapi.vehicle.chassis}", example = "9BWZZZ372VT004251")
                @Chassis @Nullable String chassis,
        @Schema(description = "${openapi.vehicle.brand}", example = "Volkswagen")
                @NotBlank @Size(max = 50, message = "{validation.maxLength}") String brand,
        @Schema(description = "${openapi.vehicle.model}", example = "Gol")
                @NotBlank @Size(max = 80, message = "{validation.maxLength}") String model,
        @Schema(description = "${openapi.vehicle.year}", example = "2020") @NotNull @VehicleYear Integer year,
        @Schema(description = "${openapi.vehicle.color}", example = "Prata")
                @NotBlank @Size(max = 30, message = "{validation.maxLength}") String color) {
}
