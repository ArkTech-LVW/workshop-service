package br.com.arktech.workshop.shared.api.validation;

import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class ValidLicensePlateValidator implements ConstraintValidator<ValidLicensePlate, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || LicensePlate.isValid(value);
    }
}
