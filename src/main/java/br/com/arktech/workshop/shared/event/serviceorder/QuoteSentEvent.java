package br.com.arktech.workshop.shared.event.serviceorder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteSentEvent(
        Long serviceOrderId, UUID publicId, Long customerId, BigDecimal totalPrice, List<PartQuantity> parts) {

    public QuoteSentEvent {
        parts = List.copyOf(parts);
    }
}
