package br.com.arktech.workshop.inventory.application.releaseserviceorderreservation;

import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuoteRejectedListener {

    private final ReleaseServiceOrderReservationUseCase releaseServiceOrderReservationUseCase;

    @EventListener
    void on(QuoteRejectedEvent event) {
        releaseServiceOrderReservationUseCase.execute(new ReleaseServiceOrderReservationCommand(event.serviceOrderId()));
    }
}
