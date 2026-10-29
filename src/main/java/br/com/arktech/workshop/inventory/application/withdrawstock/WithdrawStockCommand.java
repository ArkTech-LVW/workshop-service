package br.com.arktech.workshop.inventory.application.withdrawstock;

import java.util.List;

import org.jspecify.annotations.Nullable;

/** The user is null when the customer approves the quote without logging in. */
public record WithdrawStockCommand(Long serviceOrderId, List<Item> items, @Nullable Long userId) {

    public WithdrawStockCommand {
        items = List.copyOf(items);
    }

    public record Item(Long partId, int quantity) {
    }
}
