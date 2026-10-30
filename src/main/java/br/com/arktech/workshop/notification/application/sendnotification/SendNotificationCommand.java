package br.com.arktech.workshop.notification.application.sendnotification;

import br.com.arktech.workshop.notification.domain.model.NotificationType;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record SendNotificationCommand(
        NotificationType type,
        UUID serviceOrderPublicId,
        Long customerId,
        BigDecimal totalPrice,
        @Nullable String reason) {
}
