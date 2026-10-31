package br.com.arktech.workshop.shared.application.i18n;

public final class DurationFormatter {

    private DurationFormatter() {}

    public static String formatMinutes(long totalMinutes) {
        return "%02d:%02d".formatted(totalMinutes / 60, totalMinutes % 60);
    }
}
