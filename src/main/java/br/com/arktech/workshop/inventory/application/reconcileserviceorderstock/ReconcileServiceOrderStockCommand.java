package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record ReconcileServiceOrderStockCommand(
        Long serviceOrderId, List<ServiceOrderPart> parts, @Nullable Long userId) {

    public ReconcileServiceOrderStockCommand {
        parts = List.copyOf(parts);
    }
}
