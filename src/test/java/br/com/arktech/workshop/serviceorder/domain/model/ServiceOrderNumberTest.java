package br.com.arktech.workshop.serviceorder.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.time.Year;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceOrderNumberTest {

    @Test
    void shouldFormatYearAndSequenceWithSixDigits() {
        assertThat(ServiceOrderNumber.of(Year.of(2026), 123).value()).isEqualTo("SO-2026-000123");
    }

    @Test
    void shouldKeepSequencesLongerThanSixDigits() {
        assertThat(ServiceOrderNumber.of(Year.of(2026), 1_234_567).value()).isEqualTo("SO-2026-1234567");
    }

    @Test
    void shouldNormalizeCaseAndSpaces() {
        assertThat(new ServiceOrderNumber(" so-2026-000001 ").value()).isEqualTo("SO-2026-000001");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "SO-2026-00001", "SO-26-000001", "OS-2026-000001", "SO2026000001", "SO-2026-0000000000001"
    })
    void shouldRejectInvalidFormat(String value) {
        assertThatThrownBy(() -> new ServiceOrderNumber(value))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrderNumber.invalid");
    }

    @Test
    void shouldRejectNegativeSequence() {
        assertThatThrownBy(() -> ServiceOrderNumber.of(Year.of(2026), -1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrderNumber.invalid");
    }
}
