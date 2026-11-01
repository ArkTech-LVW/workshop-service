package br.com.arktech.workshop.notification.domain.repository;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.model.EmailNotificationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryEmailNotificationRepository implements EmailNotificationRepository {

    private final List<EmailNotification> notifications = new ArrayList<>();

    private long nextId = 1;

    @Override
    public EmailNotification save(EmailNotification emailNotification) {
        if (!notifications.contains(emailNotification)) {
            ReflectionTestUtils.setField(emailNotification, "id", nextId++);
            notifications.add(emailNotification);
        }
        return emailNotification;
    }

    @Override
    public List<EmailNotification> lockPendingDueAt(Instant now, int limit) {
        return notifications.stream()
                .filter(email -> email.getStatus() == EmailNotificationStatus.PENDING)
                .filter(email -> !Objects.requireNonNull(email.getNextAttemptAt()).isAfter(now))
                .sorted(Comparator.comparing(email -> Objects.requireNonNull(email.getNextAttemptAt())))
                .limit(limit)
                .toList();
    }

    public List<EmailNotification> findAll() {
        return List.copyOf(notifications);
    }
}
