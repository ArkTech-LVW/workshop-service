package br.com.arktech.workshop.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.email.Email;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class EmailNotificationTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 3;

    @Test
    void shouldMarkTheEmailAsSent() {
        EmailNotification email = pending();

        email.markSent(NOW.plusSeconds(30));

        assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.SENT);
        assertThat(email.getAttempts()).isOne();
        assertThat(email.getSentAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(email.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldScheduleTheNextAttemptLaterAfterEachFailure() {
        EmailNotification email = pending();

        email.registerFailure("Connection refused", NOW, MAX_ATTEMPTS, RETRY_DELAY);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));

        email.registerFailure("Connection refused", NOW, MAX_ATTEMPTS, RETRY_DELAY);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));

        assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
        assertThat(email.getAttempts()).isEqualTo(2);
        assertThat(email.getLastError()).isEqualTo("Connection refused");
    }

    @Test
    void shouldFailTheEmailOnTheLastAttempt() {
        EmailNotification email = pending();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            email.registerFailure("Connection refused", NOW, MAX_ATTEMPTS, RETRY_DELAY);
        }

        assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.FAILED);
        assertThat(email.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(email.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldNotSendAnEmailThatIsNoLongerPending() {
        EmailNotification email = EmailNotification.withoutRecipient(10L, "TYPE", "Assunto", "Corpo", "No email");

        assertThatThrownBy(() -> email.markSent(NOW)).isInstanceOf(IllegalStateException.class);
    }

    private static EmailNotification pending() {
        return EmailNotification.pending(10L, "TYPE", new Email("ana@email.com"), "Assunto", "Corpo", NOW);
    }
}
