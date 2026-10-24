package br.com.arktech.workshop.shared.domain.document;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import java.util.regex.Pattern;

public record Cpf(String value) implements CpfCnpj {

    static final int LENGTH = 11;

    private static final Pattern CHARACTERS = Pattern.compile("\\d{11}");
    private static final int[] FIRST_WEIGHTS = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final Pattern MASK_GROUPS = Pattern.compile("(\\d{3})(\\d{3})(\\d{3})(\\d{2})");
    private static final String MASK = "$1.$2.$3-$4";

    public Cpf {
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
        return PersonType.NATURAL_PERSON;
    }

    @Override
    public String formatted() {
        return MASK_GROUPS.matcher(value).replaceFirst(MASK);
    }
}
