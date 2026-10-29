package br.com.arktech.workshop.serviceorder.application.addpartitem;

import br.com.arktech.workshop.serviceorder.domain.model.PartItem;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

public record AddPartItemResult(
        Long id,
        Long partId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal serviceOrderTotalPrice,
        int availableQuantity,
        boolean insufficientStock) {

    static AddPartItemResult from(PartItem item, Money serviceOrderTotalPrice, int availableQuantity, boolean insufficientStock) {
        return new AddPartItemResult(
                item.getId(),
                item.getPartId(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount(),
                serviceOrderTotalPrice.amount(),
                availableQuantity,
                insufficientStock);
    }
}
