package br.com.arktech.workshop.notification.application.email;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.model.EmailNotificationStatus;
import br.com.arktech.workshop.notification.domain.repository.InMemoryEmailNotificationRepository;
import br.com.arktech.workshop.shared.application.i18n.TestMessageResolver;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class EmailChannelTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Long CUSTOMER_ID = 10L;
    private static final Email ANA = new Email("ana@email.com");

    private final InMemoryEmailNotificationRepository repository = new InMemoryEmailNotificationRepository();
    private final Map<Long, Email> emails = Map.of(CUSTOMER_ID, ANA);
    private final EmailChannel channel = new EmailChannel(
            new EmailTemplate(TestMessageResolver.withApplicationMessages()),
            customerId -> Optional.ofNullable(emails.get(customerId)),
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldRecordAPendingEmailWithTheTemplateOfTheType() {
        channel.record(new Notification(
                CUSTOMER_ID,
                NotificationType.SERVICE_ORDER_QUOTE_REJECTED,
                List.of("OS-1", "R$ 64,00", "Achou caro")));

        assertThat(repository.findAll()).singleElement().satisfies(email -> {
            assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
            assertThat(email.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(email.getType()).isEqualTo("SERVICE_ORDER_QUOTE_REJECTED");
            assertThat(email.getRecipient()).isEqualTo(ANA);
            assertThat(email.getSubject()).isEqualTo("Orçamento da ordem de serviço OS-1 rejeitado");
            assertThat(email.getBody())
                    .isEqualTo("O orçamento da ordem de serviço OS-1 foi rejeitado. Valor total: R$ 64,00. "
                            + "Motivo: Achou caro");
            assertThat(email.getAttempts()).isZero();
            assertThat(email.getNextAttemptAt()).isEqualTo(NOW);
        });
    }

    @Test
    void shouldRecordAFailedEmailWhenTheCustomerHasNoEmail() {
        channel.record(new Notification(
                99L, NotificationType.SERVICE_ORDER_COMPLETED, List.of("OS-1", "R$ 64,00")));

        assertThat(repository.findAll()).singleElement().satisfies(email -> {
            assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.FAILED);
            assertThat(email.getRecipient()).isNull();
            assertThat(email.getNextAttemptAt()).isNull();
            assertThat(email.getLastError()).isEqualTo(EmailChannel.RECIPIENT_NOT_FOUND);
            assertThat(email.getSubject()).isEqualTo("Ordem de serviço OS-1 finalizada");
        });
    }

    @Test
    void shouldHaveATemplateForEveryNotificationType() {
        for (NotificationType type : NotificationType.values()) {
            channel.record(new Notification(CUSTOMER_ID, type, List.of("OS-1", "R$ 64,00", "Motivo")));
        }

        assertThat(repository.findAll())
                .extracting(EmailNotification::getSubject)
                .hasSize(NotificationType.values().length)
                .allSatisfy(subject -> assertThat(subject).contains("OS-1").doesNotStartWith("notification."));
    }
}
