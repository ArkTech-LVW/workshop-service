package br.com.arktech.workshop.shared.domain.document;

import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

// Modulo 11 shared by CPF and CNPJ. Each character counts as its ASCII code minus 48,
// so digits keep their value and the letters of the alphanumeric CNPJ count from 17 ('A').
final class CheckDigits {

    private static final Pattern SEPARATORS = Pattern.compile("[.\\-/]");

    private CheckDigits() {}

    static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        return SEPARATORS.matcher(input.strip()).replaceAll("").toUpperCase(Locale.ROOT);
    }

    static boolean isRepeated(String value) {
        return value.chars().distinct().count() == 1;
    }

    static boolean matches(String value, int[] firstWeights, int[] secondWeights) {
        int position = firstWeights.length;
        return digitAt(value, position) == compute(value, firstWeights)
                && digitAt(value, position + 1) == compute(value, secondWeights);
    }

    private static int compute(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digitAt(value, i) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String value, int index) {
        return value.charAt(index) - '0';
    }
}
