package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.shared.api.validation.Chassis;
import br.com.arktech.workshop.shared.api.validation.LicensePlate;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

record UpdateVehicleRequest(
        @Schema(description = "${openapi.vehicle.licensePlate}", example = "ABC-1D23")
                @NotBlank @LicensePlate String licensePlate,
        @Schema(description = "${openapi.vehicle.chassis}", example = "9BWZZZ372VT004251")
                @Chassis @Nullable String chassis,
        @Schema(description = "${openapi.vehicle.color}", example = "Prata")
                @NotBlank @Size(max = 30, message = "{validation.maxLength}") String color) {
}
