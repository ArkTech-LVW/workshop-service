package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;

import org.jspecify.annotations.Nullable;

public record PartFilter(@Nullable String name, @Nullable Sku sku, @Nullable PartType type) {
}
