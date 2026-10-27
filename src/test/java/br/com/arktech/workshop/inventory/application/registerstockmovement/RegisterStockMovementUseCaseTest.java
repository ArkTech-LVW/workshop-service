package br.com.arktech.workshop.inventory.application.registerstockmovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RegisterStockMovementUseCaseTest {

    private static final Long USER_ID = 7L;

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final RegisterStockMovementUseCase registerStockMovementUseCase =
            new RegisterStockMovementUseCase(partRepository, stockMovementRepository, () -> USER_ID);

    @Test
    void shouldRegisterEntryByCurrentUser() {
        Part part = savePart();

        StockMovementResult result = registerStockMovementUseCase.execute(
                new RegisterStockMovementCommand(part.getId(), StockMovementType.IN, 10, "NF 1234"));

        assertThat(result.partId()).isEqualTo(part.getId());
        assertThat(result.type()).isEqualTo(StockMovementType.IN);
        assertThat(result.quantity()).isEqualTo(10);
        assertThat(result.reason()).isEqualTo("NF 1234");
        assertThat(result.createdByUserId()).isEqualTo(USER_ID);
        assertThat(part.getStockQuantity()).isEqualTo(10);
        assertThat(stockMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldRegisterAdjustment() {
        Part part = savePart();
        part.increaseStock(10);

        registerStockMovementUseCase.execute(
                new RegisterStockMovementCommand(part.getId(), StockMovementType.ADJUSTMENT, 3, "Avaria"));

        assertThat(part.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void shouldRejectAdjustmentGreaterThanStockWithoutRecordingIt() {
        Part part = savePart();
        part.increaseStock(2);

        assertThatThrownBy(() -> registerStockMovementUseCase.execute(
                        new RegisterStockMovementCommand(part.getId(), StockMovementType.ADJUSTMENT, 3, "Avaria")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(part.getStockQuantity()).isEqualTo(2);
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectManualOut() {
        Part part = savePart();

        assertThatThrownBy(() -> registerStockMovementUseCase.execute(
                        new RegisterStockMovementCommand(part.getId(), StockMovementType.OUT, 1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.outRequiresServiceOrder");
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> registerStockMovementUseCase.execute(
                        new RegisterStockMovementCommand(1234L, StockMovementType.IN, 1, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part savePart() {
        return partRepository.save(new Part(
                new Sku("FILTRO-01"), "Filtro", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
    }
}
