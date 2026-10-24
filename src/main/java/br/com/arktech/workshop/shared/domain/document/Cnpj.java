package br.com.arktech.workshop.shared.domain.document;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import java.util.regex.Pattern;

// Accepts the alphanumeric CNPJ (Receita Federal, from July 2026): letters in the first 12 positions,
// numeric check digits.
public record Cnpj(String value) implements CpfCnpj {

    static final int LENGTH = 14;

    private static final Pattern CHARACTERS = Pattern.compile("[0-9A-Z]{12}\\d{2}");
    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final Pattern MASK_GROUPS = Pattern.compile("(\\w{2})(\\w{3})(\\w{3})(\\w{4})(\\d{2})");
    private static final String MASK = "$1.$2.$3/$4-$5";

    public Cnpj {
        value = CheckDigits.normalize(value);
        if (!isValid(value)) {
            throw new BusinessRuleException("cpfCnpj.invalid");
        }
    }

    static boolean isValid(String normalized) {
        return CHARACTERS.matcher(normalized).matches()
                && !CheckDigits.isRepeated(normalized)
                && CheckDigits.matches(normalized, FIRST_WEIGHTS, SECOND_WEIGHTS);
    }

    @Override
    public PersonType personType() {
        return PersonType.LEGAL_PERSON;
    }

    @Override
    public String formatted() {
        return MASK_GROUPS.matcher(value).replaceFirst(MASK);
    }
}
