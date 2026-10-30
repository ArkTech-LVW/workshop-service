package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderItem;

import java.math.BigDecimal;

public record QuoteItemResult(String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    static QuoteItemResult of(String name, ServiceOrderItem item) {
        return new QuoteItemResult(
                name, item.getQuantity(), item.getUnitPrice().amount(), item.getSubtotal().amount());
    }
}
