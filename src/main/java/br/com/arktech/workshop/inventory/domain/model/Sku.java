package br.com.arktech.workshop.inventory.domain.model;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9._/-]{1,50}");

    public Sku {
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleException("sku.invalid");
        }
    }
}
