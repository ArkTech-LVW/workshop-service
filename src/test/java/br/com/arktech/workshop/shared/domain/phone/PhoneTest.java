package br.com.arktech.workshop.shared.domain.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneTest {

    @ParameterizedTest
    @CsvSource({
        "11987654321,     11987654321",
        "(11) 98765-4321, 11987654321",
        "' 11 98765 4321 ', 11987654321",
        "1134567890,      1134567890",
        "(21) 2345-6789,  2123456789",
        "85.3456.7890,    8534567890"
    })
    void shouldAcceptMobileAndLandlineWithOrWithoutMask(String input, String value) {
        assertThat(Phone.isValid(input)).isTrue();
        assertThat(new Phone(input).value()).isEqualTo(value);
    }

    @Test
    void shouldBeEqualRegardlessOfMask() {
        assertThat(new Phone("(11) 98765-4321")).isEqualTo(new Phone("11987654321"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "   ",
                "98765-4321",
                "(11) 9876-5432",
                "(11) 8765-43210",
                "(11) 6765-4321",
                "(01) 98765-4321",
                "(10) 98765-4321",
                "(11) 98765-43210",
                "+55 11 98765-4321",
                "(11) 98765-432A"
            })
    void shouldRejectInvalidPhone(String input) {
        assertThat(Phone.isValid(input)).isFalse();
        assertThatThrownBy(() -> new Phone(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("phone.invalid");
    }

    @Test
    void shouldNotAcceptNull() {
        assertThat(Phone.isValid(null)).isFalse();
    }

    @Test
    void shouldConvertToAndFromColumn() {
        PhoneConverter converter = new PhoneConverter();

        assertThat(converter.convertToDatabaseColumn(new Phone("(11) 98765-4321"))).isEqualTo("11987654321");
        assertThat(converter.convertToEntityAttribute("11987654321")).isEqualTo(new Phone("11987654321"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
