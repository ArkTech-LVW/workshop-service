package br.com.arktech.workshop.serviceorder.domain.model;

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
        return this == RECEIVED
                || this == IN_DIAGNOSIS
                || this == AWAITING_APPROVAL
                || this == QUOTE_REJECTED
                || this == AWAITING_SUPPLEMENTARY_APPROVAL
                || this == SUPPLEMENTARY_REJECTED;
    }

    public boolean showsQuoteToCustomer() {
        return this == AWAITING_APPROVAL
                || this == QUOTE_APPROVED
                || this == QUOTE_REJECTED
                || this == IN_PROGRESS
                || this == AWAITING_SUPPLEMENTARY_APPROVAL
                || this == SUPPLEMENTARY_APPROVED
                || this == SUPPLEMENTARY_REJECTED
                || this == COMPLETED
                || this == DELIVERED;
    }
}
