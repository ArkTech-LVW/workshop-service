package br.com.arktech.workshop.shared.event.serviceorder;

import java.util.UUID;

public record ServiceOrderCanceledEvent(Long serviceOrderId, UUID publicId, Long canceledByUserId) {
}
