package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record OpenServiceOrderRequest(
        @Schema(description = "${openapi.serviceOrder.customerDocument}", example = "529.982.247-25")
                @NotBlank @CPFOrCNPJ String customerDocument,
        @Schema(description = "${openapi.serviceOrder.vehicle}") @NotNull @Valid ServiceOrderVehicleRequest vehicle,
        @Schema(description = "${openapi.serviceOrder.problemDescription}", example = "Barulho ao frear")
                @NotBlank @Size(max = 2000, message = "{validation.maxLength}") String problemDescription) {
}
