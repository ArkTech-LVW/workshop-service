package br.com.arktech.workshop.shared.application.notification;

import java.util.List;

public record Notification(Long customerId, NotificationType type, List<String> parameters) {

    public Notification {
        parameters = List.copyOf(parameters);
    }
}
