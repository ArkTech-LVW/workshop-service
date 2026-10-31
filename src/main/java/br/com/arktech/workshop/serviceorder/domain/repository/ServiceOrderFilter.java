package br.com.arktech.workshop.serviceorder.domain.repository;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record ServiceOrderFilter(
        @Nullable ServiceOrderStatus status,
        @Nullable Long customerId,
        @Nullable Long vehicleId,
        @Nullable Instant openedFrom,
        @Nullable Instant openedTo) {
}
