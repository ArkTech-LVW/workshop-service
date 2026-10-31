package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCanceledEvent;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ServiceOrderCanceledListener {

    private final ReconcileServiceOrderStockUseCase reconcileServiceOrderStockUseCase;

    @EventListener
    void on(ServiceOrderCanceledEvent event) {
        reconcileServiceOrderStockUseCase.execute(new ReconcileServiceOrderStockCommand(
                event.serviceOrderId(), List.of(), event.canceledByUserId()));
    }
}
