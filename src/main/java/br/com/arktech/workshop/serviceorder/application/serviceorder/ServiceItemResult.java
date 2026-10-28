package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;

import java.math.BigDecimal;

public record ServiceItemResult(
        Long id, Long serviceOfferingId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    static ServiceItemResult from(ServiceItem item) {
        return new ServiceItemResult(
                item.getId(),
                item.getServiceOfferingId(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount());
    }
}
