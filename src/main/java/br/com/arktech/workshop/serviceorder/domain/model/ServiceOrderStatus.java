package br.com.arktech.workshop.serviceorder.domain.model;

import java.util.Set;

public enum ServiceOrderStatus {
    RECEIVED,
    IN_DIAGNOSIS,
    AWAITING_APPROVAL,
    QUOTE_APPROVED,
    QUOTE_REJECTED,
    IN_PROGRESS,
    AWAITING_SUPPLEMENTARY_APPROVAL,
    SUPPLEMENTARY_APPROVED,
    SUPPLEMENTARY_REJECTED,
    COMPLETED,
    DELIVERED,
    CANCELED;

    public boolean allowsItemChanges() {
        return this == RECEIVED || this == IN_DIAGNOSIS || allowsSupplementaryQuote();
    }

    public boolean allowsSupplementaryQuote() {
        return this == IN_PROGRESS || this == SUPPLEMENTARY_REJECTED;
    }

    public boolean isClosed() {
        return this == DELIVERED || this == CANCELED;
    }

    public boolean allowsCancellation() {
        return Set.of(
                        RECEIVED,
                        IN_DIAGNOSIS,
                        AWAITING_APPROVAL,
                        QUOTE_REJECTED,
                        AWAITING_SUPPLEMENTARY_APPROVAL,
                        SUPPLEMENTARY_REJECTED)
                .contains(this);
    }

    public boolean showsQuoteToCustomer() {
        return Set.of(
                        AWAITING_APPROVAL,
                        QUOTE_APPROVED,
                        QUOTE_REJECTED,
                        IN_PROGRESS,
                        AWAITING_SUPPLEMENTARY_APPROVAL,
                        SUPPLEMENTARY_APPROVED,
                        SUPPLEMENTARY_REJECTED,
                        COMPLETED,
                        DELIVERED)
                .contains(this);
    }
}
