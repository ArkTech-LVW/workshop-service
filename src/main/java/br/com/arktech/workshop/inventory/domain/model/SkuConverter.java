package br.com.arktech.workshop.inventory.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class SkuConverter implements AttributeConverter<Sku, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Sku sku) {
        return sku == null ? null : sku.value();
    }

    @Override
    public @Nullable Sku convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new Sku(value);
    }
}
