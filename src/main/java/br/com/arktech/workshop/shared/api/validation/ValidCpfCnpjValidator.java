package br.com.arktech.workshop.shared.api.validation;

import br.com.arktech.workshop.shared.domain.document.CpfCnpj;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class ValidCpfCnpjValidator implements ConstraintValidator<ValidCpfCnpj, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || CpfCnpj.isValid(value);
    }
}
