package br.com.arktech.workshop.inventory.application.createpart;

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
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CreatePartUseCaseTest {

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final CreatePartUseCase createPartUseCase = new CreatePartUseCase(partRepository);

    @Test
    void shouldCreatePartWithNormalizedSkuAndEmptyStock() {
        PartResult result = createPartUseCase.execute(new CreatePartCommand(
                " oleo-5w30-1l ", "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, new BigDecimal("45.9"), 10));

        assertThat(result)
                .isEqualTo(new PartResult(
                        1L,
                        "OLEO-5W30-1L",
                        "Óleo 5W30 1L",
                        PartType.SUPPLY,
                        MeasurementUnit.LITER,
                        new BigDecimal("45.90"),
                        0,
                        10));
        Part part = partRepository.findRequiredById(result.id());
        assertThat(part.getSku()).isEqualTo(new Sku("OLEO-5W30-1L"));
        assertThat(part.getSalePrice()).isEqualTo(new Money(new BigDecimal("45.90")));
    }

    @Test
    void shouldRejectSkuAlreadyRegisteredIgnoringCase() {
        createPartUseCase.execute(command("FILTRO-01"));

        assertThatThrownBy(() -> createPartUseCase.execute(command("filtro-01")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("part.skuAlreadyExists")
                .extracting(error -> ((ConflictException) error).getArgs())
                .isEqualTo(new Object[] {"FILTRO-01"});
        assertThat(partRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldRejectInvalidSku() {
        assertThatThrownBy(() -> createPartUseCase.execute(command("FILTRO 01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("sku.invalid");
    }

    @Test
    void shouldRejectSalePriceWithMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> createPartUseCase.execute(new CreatePartCommand(
                        "FILTRO-01", "Filtro", PartType.PART, MeasurementUnit.UNIT, new BigDecimal("10.005"), 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.tooManyDecimals");
    }

    @Test
    void shouldRejectNegativeMinimumStock() {
        assertThatThrownBy(() -> createPartUseCase.execute(new CreatePartCommand(
                        "FILTRO-01", "Filtro", PartType.PART, MeasurementUnit.UNIT, BigDecimal.TEN, -1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
        assertThat(partRepository.findAll()).isEmpty();
    }

    private static CreatePartCommand command(String sku) {
        return new CreatePartCommand(sku, "Filtro de óleo", PartType.PART, MeasurementUnit.UNIT, BigDecimal.TEN, 2);
    }
}
