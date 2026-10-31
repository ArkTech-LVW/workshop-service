package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.shared.application.i18n.DurationFormatter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

record DurationResponse(
        @Schema(description = "${openapi.metrics.durationMinutes}", example = "150") long minutes,
        @Schema(description = "${openapi.metrics.durationFormatted}", example = "02:30") String formatted) {

    static DurationResponse from(Duration duration) {
        long minutes = Math.round(duration.toSeconds() / 60.0);
        return new DurationResponse(minutes, DurationFormatter.formatMinutes(minutes));
    }

    static @Nullable DurationResponse fromNullable(@Nullable Duration duration) {
        return duration == null ? null : from(duration);
    }
}
