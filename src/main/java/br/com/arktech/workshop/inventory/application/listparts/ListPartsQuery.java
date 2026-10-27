package br.com.arktech.workshop.inventory.application.listparts;

import br.com.arktech.workshop.inventory.domain.model.PartType;

import org.jspecify.annotations.Nullable;

public record ListPartsQuery(
        @Nullable String name, @Nullable String sku, @Nullable PartType type, int page, int size) {
}
