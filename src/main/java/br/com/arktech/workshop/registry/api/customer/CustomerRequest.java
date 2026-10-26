package br.com.arktech.workshop.registry.api.customer;

import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;
import br.com.arktech.workshop.shared.api.validation.Phone;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

record CustomerRequest(
        @Schema(description = "${openapi.customer.name}", example = "Ana Souza")
                @NotBlank @Size(max = 150, message = "{validation.maxLength}") String name,
        @Schema(description = "${openapi.customer.document}", example = "529.982.247-25")
                @NotBlank @CPFOrCNPJ String document,
        @Schema(description = "${openapi.customer.email}", example = "ana@email.com")
                @NotBlank @Email @Size(max = 254, message = "{validation.maxLength}") String email,
        @Schema(description = "${openapi.customer.phone}", example = "(11) 98765-4321")
                @Phone @Nullable String phone) {
}
