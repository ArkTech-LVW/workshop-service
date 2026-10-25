package br.com.arktech.workshop.shared.domain.email;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+");

    public Email {
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleException("email.invalid");
        }
    }
}
