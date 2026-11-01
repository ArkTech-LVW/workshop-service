package br.com.arktech.workshop.notification.infrastructure.persistence;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.repository.EmailNotificationRepository;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmailNotificationJpaRepository
        extends JpaRepository<EmailNotification, Long>, EmailNotificationRepository {
}
