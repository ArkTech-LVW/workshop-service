package br.com.arktech.workshop.shared.domain.document;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import org.jspecify.annotations.Nullable;

public sealed interface CpfCnpj permits Cpf, Cnpj {

    String value();

    PersonType personType();

    String formatted();

    static CpfCnpj of(@Nullable String input) {
        String normalized = CheckDigits.normalize(input);
        return switch (normalized.length()) {
            case Cpf.LENGTH -> new Cpf(normalized);
            case Cnpj.LENGTH -> new Cnpj(normalized);
            default -> throw new BusinessRuleException("cpfCnpj.invalid");
        };
    }

    static boolean isValid(@Nullable String input) {
        String normalized = CheckDigits.normalize(input);
        return switch (normalized.length()) {
            case Cpf.LENGTH -> Cpf.isValid(normalized);
            case Cnpj.LENGTH -> Cnpj.isValid(normalized);
            default -> false;
        };
    }
}
