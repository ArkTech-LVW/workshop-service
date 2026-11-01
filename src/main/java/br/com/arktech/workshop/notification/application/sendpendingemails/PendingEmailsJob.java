package br.com.arktech.workshop.notification.application.sendpendingemails;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class PendingEmailsJob {

    private static final int EMAILS_PER_RUN = 50;

    private final SendPendingEmailsUseCase sendPendingEmailsUseCase;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration retryDelay;

    PendingEmailsJob(
            SendPendingEmailsUseCase sendPendingEmailsUseCase,
            Clock clock,
            @Value("${notification.email.max-attempts}") int maxAttempts,
            @Value("${notification.email.retry-delay}") Duration retryDelay) {
        this.sendPendingEmailsUseCase = sendPendingEmailsUseCase;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    @Scheduled(
            initialDelayString = "${notification.email.send-interval}",
            fixedDelayString = "${notification.email.send-interval}")
    void sendPendingEmails() {
        sendPendingEmailsUseCase.execute(
                new SendPendingEmailsCommand(clock.instant(), EMAILS_PER_RUN, maxAttempts, retryDelay));
    }
}
