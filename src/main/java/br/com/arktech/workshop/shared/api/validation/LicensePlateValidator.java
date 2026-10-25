package br.com.arktech.workshop.shared.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class LicensePlateValidator implements ConstraintValidator<LicensePlate, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || br.com.arktech.workshop.shared.domain.vehicle.LicensePlate.isValid(value);
    }
}
