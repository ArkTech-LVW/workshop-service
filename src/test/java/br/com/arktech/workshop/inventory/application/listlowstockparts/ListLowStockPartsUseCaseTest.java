package br.com.arktech.workshop.inventory.application.listlowstockparts;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ListLowStockPartsUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final ListLowStockPartsUseCase listLowStockPartsUseCase = new ListLowStockPartsUseCase(partRepository);

    @Test
    void shouldListPartsAtOrBelowMinimumStockOrderedByName() {
        savePart("Vela", 1, 2);
        savePart("Filtro", 3, 3);
        savePart("Óleo", 4, 3);

        Page<PartResult> page = listLowStockPartsUseCase.execute(new ListLowStockPartsQuery(0, 20));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(PartResult::name).containsExactly("Filtro", "Vela");
    }

    private void savePart(String name, int stockQuantity, int minimumStock) {
        Part part = partRepository.save(new Part(
                new Sku(name.toUpperCase().replace("Ó", "O")),
                name,
                PartType.PART,
                MeasurementUnit.UNIT,
                new Money(BigDecimal.TEN),
                minimumStock));
        part.increaseStock(stockQuantity);
    }
}
