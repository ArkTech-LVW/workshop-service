package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuoteApprovedListener {

    private final ReconcileServiceOrderStockUseCase reconcileServiceOrderStockUseCase;

    @EventListener
    void on(QuoteApprovedEvent event) {
        reconcileServiceOrderStockUseCase.execute(new ReconcileServiceOrderStockCommand(
                event.serviceOrderId(),
                event.parts().stream()
                        .map(part -> new ReconcileServiceOrderStockCommand.Item(part.partId(), part.quantity()))
                        .toList(),
                event.approvedByUserId()));
    }
}
