package br.com.arktech.workshop.notification.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    QUOTE_SENT("notification.quoteSent"),
    QUOTE_APPROVED("notification.quoteApproved"),
    QUOTE_REJECTED("notification.quoteRejected"),
    SERVICE_ORDER_COMPLETED("notification.serviceOrderCompleted");

    private final String messageKey;
}
