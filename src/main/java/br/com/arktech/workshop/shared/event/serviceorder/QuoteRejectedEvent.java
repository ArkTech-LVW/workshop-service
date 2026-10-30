package br.com.arktech.workshop.shared.event.serviceorder;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteRejectedEvent(
        Long serviceOrderId, UUID publicId, Long customerId, BigDecimal totalPrice, String reason) {
}
