package br.com.arktech.workshop.inventory.application.withdrawstock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class WithdrawStockUseCaseTest {

    private static final Long USER_ID = 7L;
    private static final Long SERVICE_ORDER_ID = 42L;

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final WithdrawStockUseCase withdrawStockUseCase =
            new WithdrawStockUseCase(partRepository, stockMovementRepository, () -> USER_ID);

    @Test
    void shouldWithdrawEachPartAndMergeRepeatedItems() {
        Part filter = savePart("FILTRO-01", 5);
        Part oil = savePart("OLEO-5W30", 10);

        withdrawStockUseCase.execute(new WithdrawStockCommand(
                SERVICE_ORDER_ID,
                List.of(
                        new WithdrawStockCommand.Item(oil.getId(), 4),
                        new WithdrawStockCommand.Item(filter.getId(), 1),
                        new WithdrawStockCommand.Item(oil.getId(), 2))));

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
    void shouldRejectWithdrawalGreaterThanStock() {
        Part filter = savePart("FILTRO-01", 1);

        assertThatThrownBy(() -> withdrawStockUseCase.execute(new WithdrawStockCommand(
                        SERVICE_ORDER_ID, List.of(new WithdrawStockCommand.Item(filter.getId(), 2)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(filter.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> withdrawStockUseCase.execute(
                        new WithdrawStockCommand(SERVICE_ORDER_ID, List.of(new WithdrawStockCommand.Item(1234L, 1)))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part savePart(String sku, int stockQuantity) {
        Part part = partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stockQuantity);
        return part;
    }
}
