package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockReservationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ExpiredStockReservationsJobTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    private final List<ReleaseExpiredStockReservationsCommand> commands = new ArrayList<>();
    private final ExpiredStockReservationsJob job = new ExpiredStockReservationsJob(
            new ReleaseExpiredStockReservationsUseCase(
                    new InMemoryPartRepository(), new InMemoryStockReservationRepository()) {
                @Override
                public void execute(ReleaseExpiredStockReservationsCommand command) {
                    commands.add(command);
                }
            },
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldReleaseAtMostFiveHundredPartsPerRun() {
        job.releaseExpiredReservations();

        assertThat(commands).containsExactly(new ReleaseExpiredStockReservationsCommand(NOW, 500));
    }
}
