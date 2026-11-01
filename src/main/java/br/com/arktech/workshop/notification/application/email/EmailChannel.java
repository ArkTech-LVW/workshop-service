package br.com.arktech.workshop.notification.application.email;

import br.com.arktech.workshop.notification.application.NotificationChannel;
import br.com.arktech.workshop.notification.application.email.EmailTemplate.RenderedEmail;
import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.repository.EmailNotificationRepository;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.registry.CustomerContacts;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class EmailChannel implements NotificationChannel {

    static final String RECIPIENT_NOT_FOUND = "Customer email not found";

    private final EmailTemplate emailTemplate;
    private final CustomerContacts customerContacts;
    private final EmailNotificationRepository emailNotificationRepository;
    private final Clock clock;

    @Override
    public void record(Notification notification) {
        RenderedEmail email = emailTemplate.render(notification);
        String type = notification.type().name();

        EmailNotification emailNotification = customerContacts
                .findEmailById(notification.customerId())
                .map(recipient -> EmailNotification.pending(
                        notification.customerId(),
                        type,
                        recipient,
                        email.subject(),
                        email.body(),
                        null,
                        clock.instant()))
                .orElseGet(() -> {
                    log.warn("Email notification without recipient [customerId={}, type={}]",
                            notification.customerId(), type);
                    return EmailNotification.withoutRecipient(
                            notification.customerId(),
                            type,
                            email.subject(),
                            email.body(),
                            null,
                            RECIPIENT_NOT_FOUND);
                });
        emailNotificationRepository.save(emailNotification);
    }
}
