package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ExpiredStockReservationsJob {

    private final ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase;
    private final Clock clock;
    private final int batchSize;

    ExpiredStockReservationsJob(
            ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase,
            Clock clock,
            @Value("${inventory.stock-reservation.release-batch-size}") int batchSize) {
        this.releaseExpiredStockReservationsUseCase = releaseExpiredStockReservationsUseCase;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${inventory.stock-reservation.release-interval}",
            fixedDelayString = "${inventory.stock-reservation.release-interval}")
    void releaseExpiredReservations() {
        Instant now = clock.instant();
        ReleaseExpiredStockReservationsResult batch;
        Long afterPartId = 0L;
        do {
            batch = releaseExpiredStockReservationsUseCase.execute(
                    new ReleaseExpiredStockReservationsCommand(now, afterPartId, batchSize));
            afterPartId = batch.lastPartId();
        } while (batch.parts() == batchSize);
    }
}
