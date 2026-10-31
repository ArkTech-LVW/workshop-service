package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuoteSentListener {

    private final ReserveServiceOrderStockUseCase reserveServiceOrderStockUseCase;

    @EventListener
    void on(QuoteSentEvent event) {
        reserveServiceOrderStockUseCase.execute(new ReserveServiceOrderStockCommand(
                event.serviceOrderId(),
                event.parts().stream()
                        .map(part -> new ServiceOrderPart(part.partId(), part.quantity()))
                        .toList()));
    }
}
