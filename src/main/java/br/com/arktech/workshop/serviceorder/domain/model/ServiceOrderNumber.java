package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.time.Year;
import java.util.Locale;
import java.util.regex.Pattern;

public record ServiceOrderNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("SO-\\d{4}-\\d{6,12}");

    public ServiceOrderNumber {
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleException("serviceOrderNumber.invalid");
        }
    }

    public static ServiceOrderNumber of(Year year, long sequence) {
        return new ServiceOrderNumber(String.format(Locale.ROOT, "SO-%04d-%06d", year.getValue(), sequence));
    }
}
