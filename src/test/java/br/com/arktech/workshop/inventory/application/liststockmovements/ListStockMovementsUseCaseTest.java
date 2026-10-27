package br.com.arktech.workshop.inventory.application.liststockmovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ListStockMovementsUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final ListStockMovementsUseCase listStockMovementsUseCase =
            new ListStockMovementsUseCase(partRepository, stockMovementRepository);

    @Test
    void shouldListOnlyMovementsOfThePartNewestFirst() {
        Part filter = savePart("FILTRO-01");
        Part oil = savePart("OLEO-5W30");
        stockMovementRepository.save(StockMovement.entry(filter, 5, "NF 1", 7L));
        stockMovementRepository.save(StockMovement.entry(oil, 5, "NF 2", 7L));
        stockMovementRepository.save(StockMovement.adjustment(filter, 1, "Avaria", 8L));

        Page<StockMovementResult> page =
                listStockMovementsUseCase.execute(new ListStockMovementsQuery(filter.getId(), 0, 20));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content())
                .extracting(StockMovementResult::reason, StockMovementResult::createdBy)
                .containsExactly(tuple("Avaria", 8L), tuple("NF 1", 7L));
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> listStockMovementsUseCase.execute(new ListStockMovementsQuery(1234L, 0, 20)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part savePart(String sku) {
        return partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
    }
}
