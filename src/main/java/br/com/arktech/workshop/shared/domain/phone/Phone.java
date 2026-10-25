package br.com.arktech.workshop.shared.domain.phone;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

public record Phone(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s().\\-]");
    private static final Pattern FORMAT = Pattern.compile("[1-9]{2}(9\\d{8}|[2-5]\\d{7})");

    public Phone {
        value = normalize(value);
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleException("phone.invalid");
        }
    }

    public static boolean isValid(@Nullable String input) {
        return FORMAT.matcher(normalize(input)).matches();
    }

    private static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        return SEPARATORS.matcher(input).replaceAll("");
    }
}
