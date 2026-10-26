package br.com.arktech.workshop.registry.application.listcustomers;

import org.jspecify.annotations.Nullable;

public record ListCustomersQuery(@Nullable String document, int page, int size) {
}
