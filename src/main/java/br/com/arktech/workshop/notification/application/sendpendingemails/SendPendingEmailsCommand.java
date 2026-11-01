package br.com.arktech.workshop.notification.application.sendpendingemails;

import java.time.Duration;
import java.time.Instant;

public record SendPendingEmailsCommand(Instant now, int limit, int maxAttempts, Duration retryDelay) {
}
