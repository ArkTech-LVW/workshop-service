package br.com.arktech.workshop.shared.domain.exception;

import java.time.Duration;

public non-sealed class TooManyRequestsException extends DomainException {

    private static final String CODE = "request.tooManyRequests";

    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super(CODE);
        this.retryAfter = retryAfter;
    }

    public long getRetryAfterSeconds() {
        return Math.max(1, retryAfter.plusNanos(999_999_999).toSeconds());
    }
}
