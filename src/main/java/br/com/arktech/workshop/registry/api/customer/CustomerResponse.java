package br.com.arktech.workshop.registry.api.customer;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.shared.domain.document.PersonType;

import io.swagger.v3.oas.annotations.media.Schema;

import org.jspecify.annotations.Nullable;

record CustomerResponse(
        @Schema(description = "${openapi.customer.id}", example = "1") Long id,
        @Schema(description = "${openapi.customer.name}", example = "Ana Souza") String name,
        @Schema(description = "${openapi.customer.document}", example = "52998224725") String document,
        @Schema(description = "${openapi.customer.personType}") PersonType personType,
        @Schema(description = "${openapi.customer.email}", example = "ana@email.com") String email,
        @Schema(description = "${openapi.customer.phone}", example = "11987654321") @Nullable String phone) {

    static CustomerResponse from(CustomerResult result) {
        return new CustomerResponse(
                result.id(),
                result.name(),
                result.document(),
                result.personType(),
                result.email(),
                result.phone());
    }
}
