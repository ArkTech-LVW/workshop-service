package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.PartItem;

import java.math.BigDecimal;

public record PartItemResult(Long id, Long partId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    static PartItemResult from(PartItem item) {
        return new PartItemResult(
                item.getId(),
                item.getPartId(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getSubtotal().amount());
    }
}
