package br.com.arktech.workshop.serviceorder.domain.model;

public enum ServiceOrderStatus {
    RECEIVED,
    IN_DIAGNOSIS,
    AWAITING_APPROVAL,
    QUOTE_APPROVED,
    QUOTE_REJECTED,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED,
    CANCELED;

    public boolean allowsItemChanges() {
        return this == RECEIVED || this == IN_DIAGNOSIS;
    }

    public boolean allowsCancellation() {
        return this == RECEIVED || this == IN_DIAGNOSIS || this == AWAITING_APPROVAL || this == QUOTE_REJECTED;
    }
}
