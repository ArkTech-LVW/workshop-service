package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;

import java.util.List;

public record ReserveServiceOrderStockCommand(Long serviceOrderId, List<ServiceOrderPart> parts) {

    public ReserveServiceOrderStockCommand {
        parts = List.copyOf(parts);
    }
}
