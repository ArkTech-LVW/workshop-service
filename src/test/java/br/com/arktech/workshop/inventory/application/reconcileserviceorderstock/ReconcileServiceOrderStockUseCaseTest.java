package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockReservationRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ReconcileServiceOrderStockUseCaseTest {

    private static final Long USER_ID = 7L;
    private static final Long SERVICE_ORDER_ID = 42L;
    private static final Long OTHER_SERVICE_ORDER_ID = 43L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-07T12:00:00Z");

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final InMemoryStockReservationRepository stockReservationRepository =
            new InMemoryStockReservationRepository();
    private final ReconcileServiceOrderStockUseCase reconcileServiceOrderStockUseCase =
            new ReconcileServiceOrderStockUseCase(partRepository, stockMovementRepository, stockReservationRepository);

    @Test
    void shouldWithdrawEachPartAndMergeRepeatedItems() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);

        reconcile(SERVICE_ORDER_ID, List.of(item(oil, 4), item(filter, 1), item(oil, 2)), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(4);
        assertThat(oil.getStockQuantity()).isEqualTo(4);
        assertThat(stockMovementRepository.findAll())
                .extracting(
                        StockMovement::getPart,
                        StockMovement::getType,
                        StockMovement::getQuantity,
                        StockMovement::getServiceOrderId,
                        StockMovement::getCreatedByUserId)
                .containsExactly(
                        tuple(filter, StockMovementType.OUT, 1, SERVICE_ORDER_ID, USER_ID),
                        tuple(oil, StockMovementType.OUT, 6, SERVICE_ORDER_ID, USER_ID));
    }

    @Test
    void shouldWithdrawWithoutUserWhenTheCustomerApproved() {
        Part filter = savePart("FILTRO-01", 5);

        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), null);

        assertThat(filter.getStockQuantity()).isEqualTo(3);
        assertThat(stockMovementRepository.findAll())
                .singleElement()
                .extracting(StockMovement::getCreatedByUserId)
                .isNull();
    }

    @Test
    void shouldNotMoveAnythingWhenTheServiceOrderAlreadyHasThePartsItNeeds() {
        Part filter = savePart("FILTRO-01", 5);
        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID);

        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), null);

        assertThat(filter.getStockQuantity()).isEqualTo(3);
        assertThat(stockMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldWithdrawOnlyTheDifferenceWhenTheQuantityGrows() {
        Part filter = savePart("FILTRO-01", 5);
        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID);

        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 3)), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(2);
        assertThat(stockMovementRepository.findAll())
                .extracting(StockMovement::getType, StockMovement::getQuantity)
                .containsExactly(tuple(StockMovementType.OUT, 2), tuple(StockMovementType.OUT, 1));
    }

    @Test
    void shouldReturnTheDifferenceWhenThePartLeavesTheServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2), item(oil, 4)), USER_ID);

        reconcile(SERVICE_ORDER_ID, List.of(item(oil, 1)), null);

        assertThat(filter.getStockQuantity()).isEqualTo(5);
        assertThat(oil.getStockQuantity()).isEqualTo(9);
        assertThat(stockMovementRepository.findAll())
                .extracting(
                        StockMovement::getPart,
                        StockMovement::getType,
                        StockMovement::getQuantity,
                        StockMovement::getServiceOrderId,
                        StockMovement::getCreatedByUserId)
                .containsExactly(
                        tuple(filter, StockMovementType.OUT, 2, SERVICE_ORDER_ID, USER_ID),
                        tuple(oil, StockMovementType.OUT, 4, SERVICE_ORDER_ID, USER_ID),
                        tuple(filter, StockMovementType.IN, 2, SERVICE_ORDER_ID, null),
                        tuple(oil, StockMovementType.IN, 3, SERVICE_ORDER_ID, null));
    }

    @Test
    void shouldReturnEverythingWhenNothingIsLeftInTheServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID);

        reconcile(SERVICE_ORDER_ID, List.of(), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldIgnoreTheMovementsOfOtherServiceOrders() {
        Part filter = savePart("FILTRO-01", 5);
        reconcile(OTHER_SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID);

        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 1)), USER_ID);
        reconcile(SERVICE_ORDER_ID, List.of(), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectWithdrawalGreaterThanStock() {
        Part filter = savePart("FILTRO-01", 1);

        assertThatThrownBy(() -> reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(filter.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void shouldTurnTheReservationIntoTheWithdrawal() {
        Part filter = savePart("FILTRO-01", 5);
        reserve(SERVICE_ORDER_ID, filter, 5);

        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 5)), USER_ID);

        assertThat(filter.getStockQuantity()).isZero();
        assertThat(filter.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll()).isEmpty();
        assertThat(stockMovementRepository.findAll())
                .extracting(StockMovement::getType, StockMovement::getQuantity)
                .containsExactly(tuple(StockMovementType.OUT, 5));
    }

    @Test
    void shouldReleaseTheReservationOfAPartThatLeftTheServiceOrderWithoutMovingIt() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reserve(SERVICE_ORDER_ID, filter, 2);
        reserve(SERVICE_ORDER_ID, oil, 4);

        reconcile(SERVICE_ORDER_ID, List.of(item(oil, 4)), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(5);
        assertThat(filter.getReservedQuantity()).isZero();
        assertThat(oil.getStockQuantity()).isEqualTo(6);
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll()).isEmpty();
        assertThat(stockMovementRepository.findAll()).extracting(StockMovement::getPart).containsExactly(oil);
    }

    @Test
    void shouldReleaseTheReservationAndReturnWhatLeftWhenTheServiceOrderIsCanceled() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);
        reconcile(SERVICE_ORDER_ID, List.of(item(filter, 2)), USER_ID);
        reserve(SERVICE_ORDER_ID, oil, 4);

        reconcile(SERVICE_ORDER_ID, List.of(), USER_ID);

        assertThat(filter.getStockQuantity()).isEqualTo(5);
        assertThat(oil.getStockQuantity()).isEqualTo(10);
        assertThat(oil.getReservedQuantity()).isZero();
        assertThat(stockReservationRepository.findAll()).isEmpty();
    }

    @Test
    void shouldNotTakeTheReservationOfAnotherServiceOrder() {
        Part filter = savePart("FILTRO-01", 5);
        reserve(OTHER_SERVICE_ORDER_ID, filter, 3);

        assertThatThrownBy(() -> reconcile(SERVICE_ORDER_ID, List.of(item(filter, 3)), USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(filter.getStockQuantity()).isEqualTo(5);
        assertThat(filter.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> reconcile(
                        SERVICE_ORDER_ID, List.of(new ReconcileServiceOrderStockCommand.Item(1234L, 1)), USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private void reconcile(
            Long serviceOrderId, List<ReconcileServiceOrderStockCommand.Item> items, @Nullable Long userId) {
        reconcileServiceOrderStockUseCase.execute(new ReconcileServiceOrderStockCommand(serviceOrderId, items, userId));
    }

    private void reserve(Long serviceOrderId, Part part, int quantity) {
        stockReservationRepository.save(StockReservation.reserve(part, serviceOrderId, quantity, EXPIRES_AT));
    }

    private static ReconcileServiceOrderStockCommand.Item item(Part part, int quantity) {
        return new ReconcileServiceOrderStockCommand.Item(part.getId(), quantity);
    }

    private Part savePart(String sku, int stockQuantity) {
        Part part = partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stockQuantity);
        return part;
    }
}
