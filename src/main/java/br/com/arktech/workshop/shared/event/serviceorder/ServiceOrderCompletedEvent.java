package br.com.arktech.workshop.shared.event.serviceorder;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceOrderCompletedEvent(Long serviceOrderId, UUID publicId, Long customerId, BigDecimal totalPrice) {
}
