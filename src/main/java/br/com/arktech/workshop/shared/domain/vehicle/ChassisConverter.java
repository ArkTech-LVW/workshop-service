package br.com.arktech.workshop.shared.domain.vehicle;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class ChassisConverter implements AttributeConverter<Chassis, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Chassis chassis) {
        return chassis == null ? null : chassis.value();
    }

    @Override
    public @Nullable Chassis convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new Chassis(value);
    }
}
