package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderItem;

public enum QuoteItemStage {
    COMPLETED,
    APPROVED,
    PENDING;

    static QuoteItemStage of(ServiceOrderItem item) {
        if (item instanceof ServiceItem serviceItem && serviceItem.isCompleted()) {
            return COMPLETED;
        }
        return item.isApproved() ? APPROVED : PENDING;
    }
}
