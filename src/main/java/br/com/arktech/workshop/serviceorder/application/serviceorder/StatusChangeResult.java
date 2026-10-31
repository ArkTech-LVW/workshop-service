package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record StatusChangeResult(
        @Nullable ServiceOrderStatus fromStatus,
        ServiceOrderStatus toStatus,
        @Nullable Long changedByUserId,
        @Nullable String description,
        @Nullable Instant changedAt) {

    static StatusChangeResult from(StatusChange statusChange) {
        return new StatusChangeResult(
                statusChange.getFromStatus(),
                statusChange.getToStatus(),
                statusChange.getChangedByUserId(),
                statusChange.getDescription(),
                statusChange.getDateCreated());
    }
}
