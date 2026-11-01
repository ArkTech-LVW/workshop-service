package br.com.arktech.workshop.notification.application.sendpendingemails;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.model.EmailNotificationStatus;
import br.com.arktech.workshop.notification.domain.repository.InMemoryEmailNotificationRepository;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SendPendingEmailsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Email ANA = new Email("ana@email.com");
    private static final Email BRUNO = new Email("bruno@email.com");

    private final InMemoryEmailNotificationRepository repository = new InMemoryEmailNotificationRepository();
    private final List<Email> sentTo = new ArrayList<>();
    private final SendPendingEmailsUseCase useCase =
            new SendPendingEmailsUseCase(repository, (to, subject, textBody, htmlBody) -> {
                if (to.equals(BRUNO)) {
                    throw new IllegalStateException("Mailbox unavailable");
                }
                sentTo.add(to);
            });

    @Test
    void shouldSendTheDueEmailsAndScheduleAnotherAttemptForTheFailedOnes() {
        EmailNotification toAna = repository.save(pending(ANA, NOW));
        EmailNotification toBruno = repository.save(pending(BRUNO, NOW));
        EmailNotification notDueYet = repository.save(pending(ANA, NOW.plusSeconds(60)));

        useCase.execute(new SendPendingEmailsCommand(NOW, 50, 5, Duration.ofMinutes(1)));

        assertThat(sentTo).containsExactly(ANA);
        assertThat(toAna.getStatus()).isEqualTo(EmailNotificationStatus.SENT);
        assertThat(toBruno.getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
        assertThat(toBruno.getLastError()).isEqualTo("Mailbox unavailable");
        assertThat(toBruno.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(notDueYet.getAttempts()).isZero();
    }

    @Test
    void shouldSendAtMostTheLimitPerRun() {
        repository.save(pending(ANA, NOW));
        repository.save(pending(ANA, NOW));

        useCase.execute(new SendPendingEmailsCommand(NOW, 1, 5, Duration.ofMinutes(1)));

        assertThat(sentTo).hasSize(1);
    }

    private static EmailNotification pending(Email recipient, Instant nextAttemptAt) {
        return EmailNotification.pending(
                10L, "TYPE", recipient, "Assunto", "Corpo", "<p>Corpo</p>", nextAttemptAt);
    }
}
