package br.com.arktech.workshop.registry.application.updatecustomer;

import org.jspecify.annotations.Nullable;

public record UpdateCustomerCommand(
        Long id, String name, String document, String email, @Nullable String phone) {
}
