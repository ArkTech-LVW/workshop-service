package br.com.arktech.workshop.shared.domain.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void shouldStripSpacesAndLowercase() {
        assertThat(new Email("  Ana@Oficina.COM ").value()).isEqualTo("ana@oficina.com");
    }

    @Test
    void shouldBeEqualRegardlessOfCaseAndSpaces() {
        assertThat(new Email(" ANA@oficina.com")).isEqualTo(new Email("ana@oficina.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ana", "ana@", "@oficina.com", "ana@@oficina.com", "ana silva@oficina.com"})
    void shouldRejectInvalidEmail(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("email.invalid");
    }

    @Test
    void shouldConvertToAndFromColumn() {
        EmailConverter converter = new EmailConverter();

        assertThat(converter.convertToDatabaseColumn(new Email("Ana@Oficina.com"))).isEqualTo("ana@oficina.com");
        assertThat(converter.convertToEntityAttribute("ana@oficina.com")).isEqualTo(new Email("ana@oficina.com"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
