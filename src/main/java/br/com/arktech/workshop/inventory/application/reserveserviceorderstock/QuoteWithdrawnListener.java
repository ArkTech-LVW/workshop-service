package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import br.com.arktech.workshop.shared.event.serviceorder.QuoteWithdrawnEvent;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuoteWithdrawnListener {

    private final ReserveServiceOrderStockUseCase reserveServiceOrderStockUseCase;

    @EventListener
    void on(QuoteWithdrawnEvent event) {
        reserveServiceOrderStockUseCase.execute(new ReserveServiceOrderStockCommand(event.serviceOrderId(), List.of()));
    }
}
