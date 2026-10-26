package br.com.arktech.workshop.shared.domain.money;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount.signum() < 0) {
            throw new BusinessRuleException("money.negative");
        }
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("money.tooManyDecimals");
        }
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
