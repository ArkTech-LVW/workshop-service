package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record ReconcileServiceOrderStockCommand(Long serviceOrderId, List<Item> items, @Nullable Long userId) {

    public ReconcileServiceOrderStockCommand {
        items = List.copyOf(items);
    }

    public record Item(Long partId, int quantity) {
    }
}
