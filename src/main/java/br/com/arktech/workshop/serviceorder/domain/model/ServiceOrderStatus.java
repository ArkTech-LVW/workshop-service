package br.com.arktech.workshop.serviceorder.domain.model;

import java.util.EnumSet;
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

    private static final Set<ServiceOrderStatus> QUOTE_VISIBLE_TO_CUSTOMER = EnumSet.of(
            AWAITING_APPROVAL,
            QUOTE_APPROVED,
            QUOTE_REJECTED,
            IN_PROGRESS,
            COMPLETED,
            DELIVERED);

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
        return QUOTE_VISIBLE_TO_CUSTOMER.contains(this);
    }
}
