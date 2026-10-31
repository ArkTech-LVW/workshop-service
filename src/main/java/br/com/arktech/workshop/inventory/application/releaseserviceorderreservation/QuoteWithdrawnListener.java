package br.com.arktech.workshop.inventory.application.releaseserviceorderreservation;

import br.com.arktech.workshop.shared.event.serviceorder.QuoteWithdrawnEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuoteWithdrawnListener {

    private final ReleaseServiceOrderReservationUseCase releaseServiceOrderReservationUseCase;

    @EventListener
    void on(QuoteWithdrawnEvent event) {
        releaseServiceOrderReservationUseCase.execute(new ReleaseServiceOrderReservationCommand(event.serviceOrderId()));
    }
}
