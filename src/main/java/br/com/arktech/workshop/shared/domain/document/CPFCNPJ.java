package br.com.arktech.workshop.shared.domain.document;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import org.jspecify.annotations.Nullable;

public sealed interface CPFCNPJ permits CPF, CNPJ {

    String value();

    PersonType personType();

    String formatted();

    static CPFCNPJ of(@Nullable String input) {
        String normalized = CheckDigits.normalize(input);
        return switch (normalized.length()) {
            case CPF.LENGTH -> new CPF(normalized);
            case CNPJ.LENGTH -> new CNPJ(normalized);
            default -> throw new BusinessRuleException("cpfCnpj.invalid");
        };
    }

    static boolean isValid(@Nullable String input) {
        String normalized = CheckDigits.normalize(input);
        return switch (normalized.length()) {
            case CPF.LENGTH -> CPF.isValid(normalized);
            case CNPJ.LENGTH -> CNPJ.isValid(normalized);
            default -> false;
        };
    }
}
