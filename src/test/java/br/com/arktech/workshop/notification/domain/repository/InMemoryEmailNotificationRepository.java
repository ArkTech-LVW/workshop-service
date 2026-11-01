package br.com.arktech.workshop.notification.domain.repository;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;

import java.util.ArrayList;
import java.util.List;

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

    public List<EmailNotification> findAll() {
        return List.copyOf(notifications);
    }
}
