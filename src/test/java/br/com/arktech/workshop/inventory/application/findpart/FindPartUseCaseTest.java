package br.com.arktech.workshop.inventory.application.findpart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FindPartUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final FindPartUseCase findPartUseCase = new FindPartUseCase(partRepository);

    @Test
    void shouldFindPart() {
        Part part = partRepository.save(new Part(
                new Sku("FILTRO-01"),
                "Filtro de óleo",
                PartType.PART,
                MeasurementUnit.UNIT,
                new Money(new BigDecimal("32")),
                5));

        PartResult result = findPartUseCase.execute(new FindPartQuery(part.getId()));

        assertThat(result)
                .isEqualTo(new PartResult(
                        part.getId(),
                        "FILTRO-01",
                        "Filtro de óleo",
                        PartType.PART,
                        MeasurementUnit.UNIT,
                        new BigDecimal("32.00"),
                        0,
                        0,
                        0,
                        5));
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> findPartUseCase.execute(new FindPartQuery(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }
}
