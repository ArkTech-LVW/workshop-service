package br.com.arktech.workshop.shared.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChassisTest {

    @ParameterizedTest
    @CsvSource({
        "1HGCM82633A004352,       1HGCM82633A004352",
        "1M8GDM9AXKP042788,       1M8GDM9AXKP042788",
        "9BWZZZ372VT004251,       9BWZZZ372VT004251",
        "9BD17164285123456,       9BD17164285123456",
        "11111111111111111,       11111111111111111",
        "1m8gdm9axkp042788,       1M8GDM9AXKP042788",
        "' 1HGCM 82633 A004352 ', 1HGCM82633A004352",
        "1HGCM8-2633-A004352,     1HGCM82633A004352"
    })
    void shouldAcceptValidChassisAndNormalize(String input, String value) {
        assertThat(Chassis.isValid(input)).isTrue();
        assertThat(new Chassis(input).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "9BWZZZ370VT004251",
                "1HGCM82643A004352",
                "1HGCM82633A004353",
                "1M8GDM9A1KP042788",
                "1HGCM82633A00435",
                "1HGCM82633A0043521",
                "IHGCM82633A004352",
                "1HGCM82633O004352",
                "1HGCM82633Q004352",
                "1HGCM8263.A004352",
                "",
                "   "
            })
    void shouldRejectInvalidChassis(String input) {
        assertThat(Chassis.isValid(input)).isFalse();
        assertThatThrownBy(() -> new Chassis(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("chassis.invalid");
    }

    @Test
    void shouldRejectNull() {
        assertThat(Chassis.isValid(null)).isFalse();
    }
}
