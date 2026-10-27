package br.com.arktech.workshop.shared.domain.vehicle;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public record Chassis(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]");
    private static final Pattern FORMAT = Pattern.compile("[A-HJ-NPR-Z0-9]{17}");
    private static final int[] WEIGHTS = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int CHECK_DIGIT_INDEX = 8;

    public Chassis {
        value = normalize(value);
        if (!hasValidFormatAndCheckDigit(value)) {
            throw new BusinessRuleException("chassis.invalid");
        }
    }

    public static boolean isValid(@Nullable String input) {
        return hasValidFormatAndCheckDigit(normalize(input));
    }

    private static boolean hasValidFormatAndCheckDigit(String value) {
        return FORMAT.matcher(value).matches() && value.charAt(CHECK_DIGIT_INDEX) == checkDigit(value);
    }

    private static char checkDigit(String value) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += transliterate(value.charAt(i)) * WEIGHTS[i];
        }
        int remainder = sum % 11;
        return remainder == 10 ? 'X' : (char) ('0' + remainder);
    }

    private static int transliterate(char character) {
        if (Character.isDigit(character)) {
            return character - '0';
        }
        return switch (character) {
            case 'A', 'J' -> 1;
            case 'B', 'K', 'S' -> 2;
            case 'C', 'L', 'T' -> 3;
            case 'D', 'M', 'U' -> 4;
            case 'E', 'N', 'V' -> 5;
            case 'F', 'W' -> 6;
            case 'G', 'P', 'X' -> 7;
            case 'H', 'Y' -> 8;
            case 'R', 'Z' -> 9;
            default -> throw new IllegalArgumentException("Invalid VIN character: " + character);
        };
    }

    private static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        return SEPARATORS.matcher(input).replaceAll("").toUpperCase(Locale.ROOT);
    }
}
