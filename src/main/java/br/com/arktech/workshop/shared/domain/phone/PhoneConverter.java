package br.com.arktech.workshop.shared.domain.phone;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class PhoneConverter implements AttributeConverter<Phone, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Phone phone) {
        return phone == null ? null : phone.value();
    }

    @Override
    public @Nullable Phone convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new Phone(value);
    }
}
