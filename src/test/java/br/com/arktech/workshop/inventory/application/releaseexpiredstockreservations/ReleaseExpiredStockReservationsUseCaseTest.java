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
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ReleaseExpiredStockReservationsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockReservationRepository stockReservationRepository =
            new InMemoryStockReservationRepository();
    private final ReleaseExpiredStockReservationsUseCase releaseExpiredStockReservationsUseCase =
            new ReleaseExpiredStockReservationsUseCase(partRepository, stockReservationRepository);

    @Test
    void shouldReleaseOnlyTheReservationsThatExpired() {
        Part filter = savePart("FILTRO-01", 10);
        Part oil = savePart("OLEO-5W30", 10);
        reserve(filter, 42L, 3, NOW.minus(Duration.ofMinutes(1)));
        StockReservation stillValid = reserve(filter, 43L, 2, NOW.plus(Duration.ofMinutes(1)));
        reserve(oil, 42L, 4, NOW.minus(Duration.ofDays(1)));

        release(100);

        assertThat(filter.getReservedQuantity()).isEqualTo(2);
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(filter.getStockQuantity()).isEqualTo(10);
        assertThat(stockReservationRepository.findAll()).containsExactly(stillValid);
    }

    @Test
    void shouldKeepAReservationThatExpiresExactlyNow() {
        Part filter = savePart("FILTRO-01", 10);
        reserve(filter, 42L, 3, NOW);

        release(100);

        assertThat(filter.getReservedQuantity()).isEqualTo(3);
        assertThat(stockReservationRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldDoNothingWhenNoReservationExpired() {
        Part filter = savePart("FILTRO-01", 10);
        reserve(filter, 42L, 3, NOW.plus(Duration.ofDays(7)));

        release(100);
        release(100);

        assertThat(filter.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void shouldReleaseAtMostTheLimitOfPartsInIdOrder() {
        Part filter = savePart("FILTRO-01", 10);
        Part oil = savePart("OLEO-5W30", 10);
        Part pad = savePart("PAST-01", 10);
        Instant expired = NOW.minus(Duration.ofMinutes(1));
        reserve(pad, 42L, 1, expired);
        reserve(oil, 42L, 1, expired);
        reserve(filter, 43L, 1, expired);
        reserve(filter, 44L, 2, expired);

        release(2);

        assertThat(filter.getReservedQuantity()).isZero();
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(pad.getReservedQuantity()).isOne();

        release(2);

        assertThat(pad.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll()).isEmpty();
    }

    private void release(int limit) {
        releaseExpiredStockReservationsUseCase.execute(new ReleaseExpiredStockReservationsCommand(NOW, limit));
    }

    private StockReservation reserve(Part part, Long serviceOrderId, int quantity, Instant expiresAt) {
        return stockReservationRepository.save(StockReservation.reserve(part, serviceOrderId, quantity, expiresAt));
    }

    private Part savePart(String sku, int stockQuantity) {
        Part part = partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stockQuantity);
        return part;
    }
}
