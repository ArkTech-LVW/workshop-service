package br.com.arktech.workshop.shared.api.validation;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class CPFOrCNPJValidator implements ConstraintValidator<CPFOrCNPJ, String> {

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || CPFCNPJ.isValid(value);
    }
}
