package br.com.arktech.workshop.shared.domain.vehicle;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public record LicensePlate(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]");
    private static final Pattern FORMAT = Pattern.compile("[A-Z]{3}\\d[A-Z0-9]\\d{2}");

    public LicensePlate {
        value = normalize(value);
        if (!FORMAT.matcher(value).matches()) {
            throw new BusinessRuleException("licensePlate.invalid");
        }
    }

    public static boolean isValid(@Nullable String input) {
        return FORMAT.matcher(normalize(input)).matches();
    }

    private static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        return SEPARATORS.matcher(input).replaceAll("").toUpperCase(Locale.ROOT);
    }
}
