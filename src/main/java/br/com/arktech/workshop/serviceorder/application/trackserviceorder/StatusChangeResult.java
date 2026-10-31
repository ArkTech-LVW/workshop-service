package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record StatusChangeResult(ServiceOrderStatus status, @Nullable String description, @Nullable Instant date) {

    static StatusChangeResult from(StatusChange change) {
        return new StatusChangeResult(change.getToStatus(), change.getDescription(), change.getDateCreated());
    }
}
