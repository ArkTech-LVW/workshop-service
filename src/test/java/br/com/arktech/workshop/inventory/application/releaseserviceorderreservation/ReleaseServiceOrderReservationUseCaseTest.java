package br.com.arktech.workshop.inventory.application.releaseserviceorderreservation;

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
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ReleaseServiceOrderReservationUseCaseTest {

    private static final Long SERVICE_ORDER_ID = 42L;
    private static final Long OTHER_SERVICE_ORDER_ID = 43L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-07T12:00:00Z");

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockReservationRepository stockReservationRepository =
            new InMemoryStockReservationRepository();
    private final ReleaseServiceOrderReservationUseCase releaseServiceOrderReservationUseCase =
            new ReleaseServiceOrderReservationUseCase(partRepository, stockReservationRepository);

    @Test
    void shouldReleaseEveryReservationOfTheServiceOrderWithoutTouchingTheStock() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reserve(filter, SERVICE_ORDER_ID, 2);
        reserve(oil, SERVICE_ORDER_ID, 4);
        StockReservation other = reserve(filter, OTHER_SERVICE_ORDER_ID, 1);

        releaseServiceOrderReservationUseCase.execute(new ReleaseServiceOrderReservationCommand(SERVICE_ORDER_ID));

        assertThat(filter.getReservedQuantity()).isOne();
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(filter.getStockQuantity()).isEqualTo(5);
        assertThat(oil.getStockQuantity()).isEqualTo(10);
        assertThat(stockReservationRepository.findAll()).containsExactly(other);
    }

    @Test
    void shouldDoNothingWhenTheServiceOrderHasNoReservation() {
        Part filter = savePart("FILTRO-01", 5);
        reserve(filter, OTHER_SERVICE_ORDER_ID, 1);

        releaseServiceOrderReservationUseCase.execute(new ReleaseServiceOrderReservationCommand(SERVICE_ORDER_ID));

        assertThat(filter.getReservedQuantity()).isOne();
    }

    private StockReservation reserve(Part part, Long serviceOrderId, int quantity) {
        return stockReservationRepository.save(StockReservation.reserve(part, serviceOrderId, quantity, EXPIRES_AT));
    }

    private Part savePart(String sku, int stockQuantity) {
        Part part = partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stockQuantity);
        return part;
    }
}
