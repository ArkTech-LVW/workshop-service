package br.com.arktech.workshop.metrics.api.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

record DurationResponse(
        @Schema(description = "${openapi.metrics.durationMinutes}", example = "150") long minutes,
        @Schema(description = "${openapi.metrics.durationFormatted}", example = "02:30") String formatted) {

    static DurationResponse from(Duration duration) {
        long minutes = Math.round(duration.toSeconds() / 60.0);
        return new DurationResponse(minutes, "%02d:%02d".formatted(minutes / 60, minutes % 60));
    }

    static @Nullable DurationResponse fromNullable(@Nullable Duration duration) {
        return duration == null ? null : from(duration);
    }
}
