package br.com.arktech.workshop.notification.infrastructure.persistence;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.repository.EmailNotificationRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

interface EmailNotificationJpaRepository
        extends JpaRepository<EmailNotification, Long>, EmailNotificationRepository {

    @Override
    @NativeQuery("""
            SELECT * FROM email_notification
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """)
    List<EmailNotification> lockPendingDueAt(@Param("now") Instant now, @Param("limit") int limit);
}
