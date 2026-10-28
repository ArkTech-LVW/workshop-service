package br.com.arktech.workshop.serviceorder.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class ServiceOrderNumberConverter implements AttributeConverter<ServiceOrderNumber, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable ServiceOrderNumber number) {
        return number == null ? null : number.value();
    }

    @Override
    public @Nullable ServiceOrderNumber convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new ServiceOrderNumber(value);
    }
}
