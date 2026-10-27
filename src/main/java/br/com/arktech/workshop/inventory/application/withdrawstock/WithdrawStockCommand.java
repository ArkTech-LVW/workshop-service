package br.com.arktech.workshop.inventory.application.withdrawstock;

import java.util.List;

public record WithdrawStockCommand(Long serviceOrderId, List<Item> items) {

    public WithdrawStockCommand {
        items = List.copyOf(items);
    }

    public record Item(Long partId, int quantity) {
    }
}
