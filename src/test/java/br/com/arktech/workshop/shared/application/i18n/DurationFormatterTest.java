package br.com.arktech.workshop.shared.application.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DurationFormatterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            0    | 00:00
            2    | 00:02
            60   | 01:00
            150  | 02:30
            1625 | 27:05
            6000 | 100:00
            """)
    void shouldFormatTheMinutesAsHoursAndMinutes(long totalMinutes, String formatted) {
        assertThat(DurationFormatter.formatMinutes(totalMinutes)).isEqualTo(formatted);
    }
}
