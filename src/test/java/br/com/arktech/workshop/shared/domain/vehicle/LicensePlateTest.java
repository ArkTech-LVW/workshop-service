package br.com.arktech.workshop.shared.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

    @ParameterizedTest
    @CsvSource({
        "ABC1234,     ABC1234",
        "ABC-1234,    ABC1234",
        "abc-1234,    ABC1234",
        "' ABC 1234 ', ABC1234",
        "ABC1D23,     ABC1D23",
        "abc1d23,     ABC1D23",
        "ABC-1D23,    ABC1D23"
    })
    void shouldAcceptValidPlatesAndNormalize(String input, String value) {
        assertThat(LicensePlate.isValid(input)).isTrue();
        assertThat(new LicensePlate(input).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "AB12345",
                "ABCD123",
                "ABC123",
                "ABC12345",
                "1BC1234",
                "ABC1DD3",
                "ABC.1234",
                "",
                "   "
            })
    void shouldRejectInvalidPlates(String input) {
        assertThat(LicensePlate.isValid(input)).isFalse();
        assertThatThrownBy(() -> new LicensePlate(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("licensePlate.invalid");
    }

    @Test
    void shouldRejectNull() {
        assertThat(LicensePlate.isValid(null)).isFalse();
    }
}
