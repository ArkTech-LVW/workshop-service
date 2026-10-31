package br.com.arktech.workshop.serviceorder.domain.model;

import java.util.Set;

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

    public boolean isClosed() {
        return this == DELIVERED || this == CANCELED;
    }

    public boolean allowsCancellation() {
        return this == RECEIVED || this == IN_DIAGNOSIS || this == AWAITING_APPROVAL || this == QUOTE_REJECTED;
    }

    public boolean showsQuoteToCustomer() {
        return Set.of(
                        AWAITING_APPROVAL,
                        QUOTE_APPROVED,
                        QUOTE_REJECTED,
                        IN_PROGRESS,
                        COMPLETED,
                        DELIVERED)
                .contains(this);
    }
}
