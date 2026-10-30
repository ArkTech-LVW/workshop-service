package br.com.arktech.workshop.shared.event.serviceorder;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record QuoteApprovedEvent(
        Long serviceOrderId, UUID publicId, List<PartQuantity> parts, @Nullable Long approvedByUserId) {

    public QuoteApprovedEvent {
        parts = List.copyOf(parts);
    }

    public record PartQuantity(Long partId, int quantity) {
    }
}
