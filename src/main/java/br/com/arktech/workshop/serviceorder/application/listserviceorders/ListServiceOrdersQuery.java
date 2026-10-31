package br.com.arktech.workshop.serviceorder.application.listserviceorders;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record ListServiceOrdersQuery(
        @Nullable ServiceOrderStatus status,
        @Nullable String customerDocument,
        @Nullable String licensePlate,
        @Nullable Instant openedFrom,
        @Nullable Instant openedTo,
        int page,
        int size) {
}
