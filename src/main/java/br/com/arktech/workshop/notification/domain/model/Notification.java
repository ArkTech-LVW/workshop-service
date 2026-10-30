package br.com.arktech.workshop.notification.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Notification(
        NotificationType type, UUID serviceOrderPublicId, Long customerId, BigDecimal totalPrice, String message) {
}
