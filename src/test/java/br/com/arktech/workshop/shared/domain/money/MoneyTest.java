package br.com.arktech.workshop.shared.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"150, 150.00", "150.5, 150.50", "150.55, 150.55", "150.5500, 150.55", "0, 0.00"})
    void shouldKeepTwoDecimalPlaces(String input, String amount) {
        assertThat(new Money(new BigDecimal(input)).amount()).isEqualTo(new BigDecimal(amount));
    }

    @Test
    void shouldBeEqualRegardlessOfScale() {
        assertThat(new Money(new BigDecimal("10"))).isEqualTo(new Money(new BigDecimal("10.000")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-150"})
    void shouldRejectNegativeAmount(String input) {
        assertThatThrownBy(() -> new Money(new BigDecimal(input)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"150.555", "0.001"})
    void shouldRejectMoreThanTwoDecimalPlaces(String input) {
        assertThatThrownBy(() -> new Money(new BigDecimal(input)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.tooManyDecimals");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "150", "150.5", "150.55", "150.5500"})
    void shouldAcceptValidAmount(String input) {
        assertThat(Money.isValid(new BigDecimal(input))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "150.555", "0.001"})
    void shouldRejectInvalidAmount(String input) {
        assertThat(Money.isValid(new BigDecimal(input))).isFalse();
    }

    @Test
    void shouldNotAcceptNullAmount() {
        assertThat(Money.isValid(null)).isFalse();
    }

    @Test
    void shouldStartFromZero() {
        assertThat(Money.ZERO.amount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldAddWithoutChangingTheOperands() {
        Money labor = money("150.50");
        Money part = money("49.50");

        assertThat(labor.plus(part)).isEqualTo(money("200.00"));
        assertThat(labor).isEqualTo(money("150.50"));
        assertThat(part).isEqualTo(money("49.50"));
    }

    @Test
    void shouldSumManyValuesFromZero() {
        Money total = Stream.of(money("10.10"), money("20.20"), money("0.70")).reduce(Money.ZERO, Money::plus);

        assertThat(total).isEqualTo(money("31.00"));
    }

    @Test
    void shouldSubtract() {
        assertThat(money("200.00").minus(money("49.50"))).isEqualTo(money("150.50"));
        assertThat(money("49.50").minus(money("49.50"))).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldRejectSubtractionBelowZero() {
        assertThatThrownBy(() -> money("10.00").minus(money("10.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.negative");
    }

    @Test
    void shouldMultiplyByQuantity() {
        assertThat(money("49.90").times(3)).isEqualTo(money("149.70"));
        assertThat(money("49.90").times(0)).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> money("49.90").times(-1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.negative");
    }

    @Test
    void shouldTellWhetherAmountIsPositive() {
        assertThat(money("0.01").isPositive()).isTrue();
        assertThat(Money.ZERO.isPositive()).isFalse();
    }

    @Test
    void shouldCompareRegardlessOfScale() {
        assertThat(money("10.01").isGreaterThan(money("10"))).isTrue();
        assertThat(money("10").isGreaterThan(money("10.00"))).isFalse();
        assertThat(money("9.99").isGreaterThan(money("10"))).isFalse();
        assertThat(money("10")).isEqualByComparingTo(money("10.00"));
        assertThat(Stream.of(money("30"), money("10"), money("20")).sorted())
                .containsExactly(money("10"), money("20"), money("30"));
    }

    @Test
    void shouldConvertToAndFromColumn() {
        MoneyConverter converter = new MoneyConverter();

        assertThat(converter.convertToDatabaseColumn(money("150.5"))).isEqualTo(new BigDecimal("150.50"));
        assertThat(converter.convertToEntityAttribute(new BigDecimal("150.50"))).isEqualTo(money("150.5"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
