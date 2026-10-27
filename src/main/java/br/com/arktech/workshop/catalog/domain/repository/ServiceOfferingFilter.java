package br.com.arktech.workshop.catalog.domain.repository;

import org.jspecify.annotations.Nullable;

public record ServiceOfferingFilter(@Nullable String name, @Nullable Boolean active) {
}
