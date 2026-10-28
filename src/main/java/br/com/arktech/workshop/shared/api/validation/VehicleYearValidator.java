package br.com.arktech.workshop.shared.api.validation;

import br.com.arktech.workshop.shared.domain.vehicle.VehicleYears;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class VehicleYearValidator implements ConstraintValidator<VehicleYear, Integer> {

    @Override
    public boolean isValid(@Nullable Integer value, ConstraintValidatorContext context) {
        return value == null || VehicleYears.isValid(value);
    }
}
