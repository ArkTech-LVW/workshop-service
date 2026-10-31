package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockReservationRepository;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ExpiredStockReservationsJobTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockReservationRepository stockReservationRepository =
            new InMemoryStockReservationRepository();
    private final List<ReleaseExpiredStockReservationsCommand> batches = new ArrayList<>();
    private final ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase =
            new ReleaseExpiredStockReservationsUseCase(partRepository, stockReservationRepository) {
                @Override
                public ReleaseExpiredStockReservationsResult execute(ReleaseExpiredStockReservationsCommand command) {
                    batches.add(command);
                    return super.execute(command);
                }
            };
    private final ExpiredStockReservationsJob job = new ExpiredStockReservationsJob(
            releaseExpiredStockReservationsUseCase, Clock.fixed(NOW, ZoneOffset.UTC), 2);

    @Test
    void shouldReleaseEverythingThatExpiredInBatchesOfTheConfiguredSize() {
        List<Part> parts = IntStream.rangeClosed(1, 5).mapToObj(this::partWithExpiredReservation).toList();

        job.releaseExpiredReservations();

        assertThat(parts).allSatisfy(part -> assertThat(part.getReservedQuantity()).isZero());
        assertThat(stockReservationRepository.findAll()).isEmpty();
        assertThat(batches)
                .extracting(ReleaseExpiredStockReservationsCommand::afterPartId)
                .containsExactly(0L, parts.get(1).getId(), parts.get(3).getId());
        assertThat(batches).allSatisfy(batch -> {
            assertThat(batch.now()).isEqualTo(NOW);
            assertThat(batch.limit()).isEqualTo(2);
        });
    }

    @Test
    void shouldStopAfterAnEmptyBatchWhenTheLastOneWasFull() {
        IntStream.rangeClosed(1, 2).forEach(this::partWithExpiredReservation);

        job.releaseExpiredReservations();

        assertThat(batches).hasSize(2);
        assertThat(stockReservationRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRunASingleBatchWhenNothingExpired() {
        job.releaseExpiredReservations();

        assertThat(batches).hasSize(1);
    }

    private Part partWithExpiredReservation(int index) {
        Part part = partRepository.save(new Part(
                new Sku("PART-" + index), "Item " + index, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(5);
        stockReservationRepository.save(
                StockReservation.reserve(part, 42L, 1, NOW.minus(Duration.ofMinutes(1))));
        return part;
    }
}
