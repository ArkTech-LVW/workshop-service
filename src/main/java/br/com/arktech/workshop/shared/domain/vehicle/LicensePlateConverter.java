package br.com.arktech.workshop.shared.domain.vehicle;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class LicensePlateConverter implements AttributeConverter<LicensePlate, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable LicensePlate licensePlate) {
        return licensePlate == null ? null : licensePlate.value();
    }

    @Override
    public @Nullable LicensePlate convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new LicensePlate(value);
    }
}
