package br.com.arktech.workshop.notification.domain.repository;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;

import java.time.Instant;
import java.util.List;

public interface EmailNotificationRepository {

    EmailNotification save(EmailNotification emailNotification);

    List<EmailNotification> lockPendingDueAt(Instant now, int limit);
}
