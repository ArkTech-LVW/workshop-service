package br.com.arktech.workshop.serviceorder.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceOrderNumberConverterTest {

    private final ServiceOrderNumberConverter converter = new ServiceOrderNumberConverter();

    @Test
    void shouldConvertToAndFromColumn() {
        assertThat(converter.convertToDatabaseColumn(new ServiceOrderNumber("SO-2026-000123")))
                .isEqualTo("SO-2026-000123");
        assertThat(converter.convertToEntityAttribute("SO-2026-000123"))
                .isEqualTo(new ServiceOrderNumber("SO-2026-000123"));
    }

    @Test
    void shouldKeepNullOnBothSides() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
