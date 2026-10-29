package br.com.arktech.workshop.shared.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class VehicleYearsTest {

    private static final int NEXT_YEAR = Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1;

    @Test
    void shouldAcceptYearsFrom1981UntilNextYearInSaoPaulo() {
        assertThat(VehicleYears.isValid(1981)).isTrue();
        assertThat(VehicleYears.isValid(NEXT_YEAR)).isTrue();
        assertThat(VehicleYears.isValid(1980)).isFalse();
        assertThat(VehicleYears.isValid(NEXT_YEAR + 1)).isFalse();
    }
}
