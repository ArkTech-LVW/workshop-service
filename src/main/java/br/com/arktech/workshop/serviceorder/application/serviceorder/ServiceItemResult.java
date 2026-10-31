package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record ServiceItemResult(
        Long id,
        Long serviceOfferingId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        @Nullable Instant approvedAt,
        @Nullable Instant completedAt) {

    static ServiceItemResult from(ServiceItem item) {
        return new ServiceItemResult(
                item.getId(),
                item.getServiceOfferingId(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount(),
                item.getApprovedAt(),
                item.getCompletedAt());
    }
}
