package br.com.arktech.workshop.notification.domain.repository;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;

public interface EmailNotificationRepository {

    EmailNotification save(EmailNotification emailNotification);
}
