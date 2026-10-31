package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import java.time.Clock;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ExpiredStockReservationsJob {

    private final ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase;
    private final Clock clock;

    @Scheduled(
            initialDelayString = "${inventory.stock-reservation.release-interval}",
            fixedDelayString = "${inventory.stock-reservation.release-interval}")
    void releaseExpiredReservations() {
        releaseExpiredStockReservationsUseCase.execute(new ReleaseExpiredStockReservationsCommand(clock.instant()));
    }
}
