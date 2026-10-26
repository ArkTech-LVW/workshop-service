package br.com.arktech.workshop.registry.application.createcustomer;

import org.jspecify.annotations.Nullable;

public record CreateCustomerCommand(String name, String document, String email, @Nullable String phone) {
}
