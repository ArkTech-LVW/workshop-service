package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.shared.api.validation.LicensePlate;
import br.com.arktech.workshop.shared.api.validation.VehicleYear;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ServiceOrderVehicleRequest(
        @Schema(description = "${openapi.vehicle.licensePlate}", example = "ABC-1D23")
                @NotBlank @LicensePlate String licensePlate,
        @Schema(description = "${openapi.vehicle.brand}", example = "Volkswagen")
                @NotBlank @Size(max = 50, message = "{validation.maxLength}") String brand,
        @Schema(description = "${openapi.vehicle.model}", example = "Gol")
                @NotBlank @Size(max = 80, message = "{validation.maxLength}") String model,
        @Schema(description = "${openapi.vehicle.year}", example = "2020") @NotNull @VehicleYear Integer year,
        @Schema(description = "${openapi.vehicle.color}", example = "Prata")
                @NotBlank @Size(max = 30, message = "{validation.maxLength}") String color) {
}
