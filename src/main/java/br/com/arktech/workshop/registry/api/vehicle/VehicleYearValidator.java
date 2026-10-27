package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.registry.domain.model.Vehicle;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

class VehicleYearValidator implements ConstraintValidator<VehicleYear, Integer> {

    @Override
    public boolean isValid(@Nullable Integer value, ConstraintValidatorContext context) {
        return value == null || Vehicle.isValidYear(value);
    }
}
