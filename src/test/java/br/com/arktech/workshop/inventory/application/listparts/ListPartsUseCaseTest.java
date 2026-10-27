package br.com.arktech.workshop.inventory.application.listparts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ListPartsUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final ListPartsUseCase listPartsUseCase = new ListPartsUseCase(partRepository);

    @Test
    void shouldListRequestedPageOrderedByName() {
        save("P-3", "Pastilha de freio", PartType.PART);
        save("P-1", "Amortecedor", PartType.PART);
        save("P-2", "Filtro de ar", PartType.PART);

        Page<PartResult> page = listPartsUseCase.execute(new ListPartsQuery(null, null, null, 1, 2));

        assertThat(page.content()).extracting(PartResult::name).containsExactly("Pastilha de freio");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldFilterByNameIgnoringCase() {
        save("F-1", "Filtro de óleo", PartType.PART);
        save("F-2", "Filtro de ar", PartType.PART);
        save("O-1", "Óleo 5W30", PartType.SUPPLY);

        Page<PartResult> page = listPartsUseCase.execute(new ListPartsQuery("FILTRO", null, null, 0, 20));

        assertThat(page.content()).extracting(PartResult::name).containsExactly("Filtro de ar", "Filtro de óleo");
    }

    @Test
    void shouldFilterBySkuIgnoringCase() {
        save("F-1", "Filtro de óleo", PartType.PART);
        save("F-2", "Filtro de ar", PartType.PART);

        Page<PartResult> page = listPartsUseCase.execute(new ListPartsQuery(null, " f-2 ", null, 0, 20));

        assertThat(page.content()).extracting(PartResult::sku).containsExactly("F-2");
    }

    @Test
    void shouldFilterByTypeCombinedWithName() {
        save("F-1", "Filtro de óleo", PartType.PART);
        save("O-1", "Óleo 5W30", PartType.SUPPLY);
        save("O-2", "Óleo de câmbio", PartType.SUPPLY);

        Page<PartResult> supplies = listPartsUseCase.execute(new ListPartsQuery(null, null, PartType.SUPPLY, 0, 20));
        Page<PartResult> oilParts = listPartsUseCase.execute(new ListPartsQuery("óleo", null, PartType.PART, 0, 20));

        assertThat(supplies.content()).extracting(PartResult::sku).containsExactly("O-1", "O-2");
        assertThat(oilParts.content()).extracting(PartResult::sku).containsExactly("F-1");
    }

    @Test
    void shouldReturnEmptyPageWhenNothingMatches() {
        save("F-1", "Filtro de óleo", PartType.PART);

        Page<PartResult> page = listPartsUseCase.execute(new ListPartsQuery(null, "X-9", null, 0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void shouldRejectInvalidSkuFilter() {
        assertThatThrownBy(() -> listPartsUseCase.execute(new ListPartsQuery(null, "F 1", null, 0, 20)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("sku.invalid");
    }

    private void save(String sku, String name, PartType type) {
        partRepository.save(new Part(
                new Sku(sku), name, type, MeasurementUnit.UNIT, new Money(new BigDecimal("10")), 0));
    }
}
