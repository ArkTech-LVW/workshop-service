package br.com.arktech.workshop.inventory.application.deletepart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DeletePartUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final DeletePartUseCase deletePartUseCase = new DeletePartUseCase(partRepository);

    @Test
    void shouldRemovePart() {
        Part filter = save("FILTRO-01");
        Part oil = save("OLEO-5W30");

        deletePartUseCase.execute(new DeletePartCommand(filter.getId()));

        assertThat(partRepository.findAll()).containsExactly(oil);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> deletePartUseCase.execute(new DeletePartCommand(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part save(String sku) {
        return partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 0));
    }
}
