package br.com.arktech.workshop.shared.domain.email;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Email email) {
        return email == null ? null : email.value();
    }

    @Override
    public @Nullable Email convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : new Email(value);
    }
}
