package br.com.arktech.workshop.notification.application.sendpendingemails;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.port.EmailSender;
import br.com.arktech.workshop.notification.domain.repository.EmailNotificationRepository;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class SendPendingEmailsIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Email ANA = new Email("ana@email.com");

    @Autowired
    private SendPendingEmailsUseCase useCase;

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void shouldSendTheDueEmailAndRecordItAsSent() {
        Long customerId = jdbcTemplate.queryForObject(
                """
                INSERT INTO customer (name, document, person_type, email, date_created, last_updated, version)
                VALUES ('Ana', ?, 'NATURAL_PERSON', 'ana@email.com', now(), now(), 0)
                RETURNING id
                """,
                Long.class,
                uniqueCPF().value());
        Long id = emailNotificationRepository
                .save(EmailNotification.pending(
                        customerId,
                        "SERVICE_ORDER_COMPLETED",
                        ANA,
                        "Ordem de serviço finalizada",
                        "Pronto para retirada",
                        "<p>Pronto para retirada</p>",
                        Instant.EPOCH))
                .getId();

        useCase.execute(new SendPendingEmailsCommand(NOW, 500, 5, Duration.ofMinutes(1)));

        verify(emailSender)
                .send(eq(ANA), eq("Ordem de serviço finalizada"), anyString(), eq("<p>Pronto para retirada</p>"));
        assertThat(jdbcTemplate.queryForMap(
                        "SELECT status, attempts, sent_at FROM email_notification WHERE id = ?", id))
                .containsEntry("status", "SENT")
                .containsEntry("attempts", 1)
                .doesNotContainEntry("sent_at", null);
    }
}
