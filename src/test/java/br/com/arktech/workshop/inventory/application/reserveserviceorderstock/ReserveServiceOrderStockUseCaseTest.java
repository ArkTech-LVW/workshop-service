package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockReservationRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReserveServiceOrderStockUseCaseTest {

    private static final Long SERVICE_ORDER_ID = 42L;
    private static final Long OTHER_SERVICE_ORDER_ID = 43L;
    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final Duration VALIDITY = Duration.ofDays(7);

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final InMemoryStockReservationRepository stockReservationRepository =
            new InMemoryStockReservationRepository();

    @Test
    void shouldReserveWhatTheServiceOrderNeedsUntilTheValidityEnds() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);

        reserve(NOW, SERVICE_ORDER_ID, item(oil, 4), item(filter, 1), item(oil, 2));

        assertThat(filter.getReservedQuantity()).isEqualTo(1);
        assertThat(oil.getReservedQuantity()).isEqualTo(6);
        assertThat(oil.getStockQuantity()).isEqualTo(10);
        assertThat(stockReservationRepository.findAll())
                .extracting(
                        StockReservation::getPart,
                        StockReservation::getServiceOrderId,
                        StockReservation::getQuantity,
                        StockReservation::getExpiresAt)
                .containsExactly(
                        tuple(filter, SERVICE_ORDER_ID, 1, NOW.plus(VALIDITY)),
                        tuple(oil, SERVICE_ORDER_ID, 6, NOW.plus(VALIDITY)));
    }

    @Test
    void shouldRefuseWhenTheAvailableStockDoesNotCoverIt() {
        Part filter = savePart("FILTRO-01", 5);
        reserve(NOW, OTHER_SERVICE_ORDER_ID, item(filter, 3));

        assertThatThrownBy(() -> reserve(NOW, SERVICE_ORDER_ID, item(filter, 3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"FILTRO-01", 2, 3});
        assertThat(filter.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void shouldReserveOnlyWhatHasNotLeftTheStockForTheServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        stockMovementRepository.save(StockMovement.withdrawal(filter, 2, SERVICE_ORDER_ID, 7L));

        reserve(NOW, SERVICE_ORDER_ID, item(filter, 3), item(oil, 1));

        assertThat(stockReservationRepository.findAll())
                .extracting(StockReservation::getPart, StockReservation::getQuantity)
                .containsExactly(tuple(filter, 1), tuple(oil, 1));
    }

    @Test
    void shouldNotReserveAPartThatAlreadyLeftTheStockForTheServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        stockMovementRepository.save(StockMovement.withdrawal(filter, 2, SERVICE_ORDER_ID, 7L));

        reserve(NOW, SERVICE_ORDER_ID, item(filter, 2));

        assertThat(filter.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll()).isEmpty();
    }

    @Test
    void shouldKeepTheQuantityAndRenewTheValidityWhenNothingChanged() {
        Part filter = savePart("FILTRO-01", 5);
        reserve(NOW, SERVICE_ORDER_ID, item(filter, 2));
        Instant later = NOW.plus(Duration.ofDays(2));

        reserve(later, SERVICE_ORDER_ID, item(filter, 2));

        assertThat(filter.getReservedQuantity()).isEqualTo(2);
        assertThat(stockReservationRepository.findAll())
                .singleElement()
                .extracting(StockReservation::getQuantity, StockReservation::getExpiresAt)
                .containsExactly(2, later.plus(VALIDITY));
    }

    @Test
    void shouldReserveOrReleaseOnlyTheDifference() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reserve(NOW, SERVICE_ORDER_ID, item(filter, 2), item(oil, 4));

        reserve(NOW, SERVICE_ORDER_ID, item(filter, 3), item(oil, 1));

        assertThat(filter.getReservedQuantity()).isEqualTo(3);
        assertThat(oil.getReservedQuantity()).isEqualTo(1);
    }

    @Test
    void shouldReleaseWhatIsNoLongerInTheServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reserve(NOW, SERVICE_ORDER_ID, item(filter, 2), item(oil, 4));
        reserve(NOW, OTHER_SERVICE_ORDER_ID, item(filter, 1));

        reserve(NOW, SERVICE_ORDER_ID);

        assertThat(filter.getReservedQuantity()).isEqualTo(1);
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll())
                .singleElement()
                .extracting(StockReservation::getServiceOrderId)
                .isEqualTo(OTHER_SERVICE_ORDER_ID);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> reserve(NOW, SERVICE_ORDER_ID, new ServiceOrderPart(1234L, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private void reserve(Instant now, Long serviceOrderId, ServiceOrderPart... items) {
        new ReserveServiceOrderStockUseCase(
                        partRepository,
                        stockMovementRepository,
                        stockReservationRepository,
                        Clock.fixed(now, ZoneOffset.UTC),
                        VALIDITY)
                .execute(new ReserveServiceOrderStockCommand(serviceOrderId, List.of(items)));
    }

    private static ServiceOrderPart item(Part part, int quantity) {
        return new ServiceOrderPart(part.getId(), quantity);
    }

    private Part savePart(String sku, int stockQuantity) {
        Part part = partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stockQuantity);
        return part;
    }
}
