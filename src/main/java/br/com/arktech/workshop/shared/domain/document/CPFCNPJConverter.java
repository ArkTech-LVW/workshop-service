package br.com.arktech.workshop.shared.domain.document;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class CPFCNPJConverter implements AttributeConverter<CPFCNPJ, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable CPFCNPJ document) {
        return document == null ? null : document.value();
    }

    @Override
    public @Nullable CPFCNPJ convertToEntityAttribute(@Nullable String value) {
        return value == null ? null : CPFCNPJ.of(value);
    }
}
