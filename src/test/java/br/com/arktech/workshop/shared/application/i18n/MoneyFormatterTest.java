package br.com.arktech.workshop.shared.application.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyFormatterTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter(Locale.of("pt", "BR"));

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            0          | R$ 0,00
            64         | R$ 64,00
            1234.5     | R$ 1.234,50
            1234567.89 | R$ 1.234.567,89
            """)
    void shouldFormatTheAmountInTheApplicationLocale(BigDecimal amount, String formatted) {
        assertThat(moneyFormatter.format(amount)).isEqualTo(formatted);
    }
}
