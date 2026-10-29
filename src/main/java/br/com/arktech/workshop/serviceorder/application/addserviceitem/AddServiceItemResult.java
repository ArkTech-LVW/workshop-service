package br.com.arktech.workshop.serviceorder.application.addserviceitem;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

public record AddServiceItemResult(
        Long id,
        Long serviceOfferingId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal serviceOrderTotalPrice) {

    static AddServiceItemResult from(ServiceItem item, Money serviceOrderTotalPrice) {
        return new AddServiceItemResult(
                item.getId(),
                item.getServiceOfferingId(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount(),
                serviceOrderTotalPrice.amount());
    }
}
