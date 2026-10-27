package br.com.arktech.workshop.shared.api.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

class ChassisValidatorTest {

    private final ChassisValidator validator = new ChassisValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    void shouldAcceptNullSoTheFieldStaysOptional() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void shouldAcceptValidChassis() {
        assertThat(validator.isValid("1hgcm8-2633-a004352", context)).isTrue();
    }

    @Test
    void shouldRejectInvalidChassis() {
        assertThat(validator.isValid("1HGCM82633A004353", context)).isFalse();
    }
}
