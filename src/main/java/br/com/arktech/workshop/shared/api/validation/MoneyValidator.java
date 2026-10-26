package br.com.arktech.workshop.shared.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public class MoneyValidator implements ConstraintValidator<Money, BigDecimal> {

    private static final int MAX_INTEGER_DIGITS = 10;

    @Override
    public boolean isValid(@Nullable BigDecimal value, ConstraintValidatorContext context) {
        return value == null
                || (br.com.arktech.workshop.shared.domain.money.Money.isValid(value) && fitsColumn(value));
    }

    private static boolean fitsColumn(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.precision() - stripped.scale() <= MAX_INTEGER_DIGITS;
    }
}
