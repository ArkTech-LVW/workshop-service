package br.com.arktech.workshop.inventory.application.updatepart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UpdatePartUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final UpdatePartUseCase updatePartUseCase = new UpdatePartUseCase(partRepository);

    @Test
    void shouldReplaceDataAndKeepStockQuantity() {
        Part part = save("OLEO-5W30");
        ReflectionTestUtils.setField(part, "stockQuantity", 7);

        PartResult result = updatePartUseCase.execute(new UpdatePartCommand(
                part.getId(),
                " oleo-5w40 ",
                "Óleo 5W40 1L",
                PartType.SUPPLY,
                MeasurementUnit.LITER,
                new BigDecimal("49.9"),
                4));

        assertThat(result)
                .isEqualTo(new PartResult(
                        part.getId(),
                        "OLEO-5W40",
                        "Óleo 5W40 1L",
                        PartType.SUPPLY,
                        MeasurementUnit.LITER,
                        new BigDecimal("49.90"),
                        7,
                        4));
        assertThat(part.getSalePrice()).isEqualTo(new Money(new BigDecimal("49.90")));
    }

    @Test
    void shouldKeepOwnSkuInAnotherCase() {
        Part part = save("OLEO-5W30");

        PartResult result = updatePartUseCase.execute(command(part.getId(), "oleo-5w30"));

        assertThat(result.sku()).isEqualTo("OLEO-5W30");
    }

    @Test
    void shouldRejectSkuOfAnotherPart() {
        save("FILTRO-01");
        Part part = save("OLEO-5W30");

        assertThatThrownBy(() -> updatePartUseCase.execute(command(part.getId(), "filtro-01")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("part.skuAlreadyExists")
                .extracting(error -> ((ConflictException) error).getArgs())
                .isEqualTo(new Object[] {"FILTRO-01"});
        assertThat(part.getSku()).isEqualTo(new Sku("OLEO-5W30"));
    }

    @Test
    void shouldRejectNegativeMinimumStock() {
        Part part = save("OLEO-5W30");

        assertThatThrownBy(() -> updatePartUseCase.execute(new UpdatePartCommand(
                        part.getId(), "OLEO-5W30", "Óleo", PartType.SUPPLY, MeasurementUnit.LITER, BigDecimal.TEN, -1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> updatePartUseCase.execute(command(1234L, "OLEO-5W30")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part save(String sku) {
        return partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
    }

    private static UpdatePartCommand command(Long id, String sku) {
        return new UpdatePartCommand(id, sku, "Item", PartType.PART, MeasurementUnit.UNIT, BigDecimal.TEN, 1);
    }
}
