package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import java.util.List;

public record ReserveServiceOrderStockCommand(Long serviceOrderId, List<Item> items) {

    public ReserveServiceOrderStockCommand {
        items = List.copyOf(items);
    }

    public record Item(Long partId, int quantity) {
    }
}
