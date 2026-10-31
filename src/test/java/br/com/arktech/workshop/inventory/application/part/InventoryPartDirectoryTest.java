package br.com.arktech.workshop.inventory.application.part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InventoryPartDirectoryTest {

    private static final Money SALE_PRICE = new Money(new BigDecimal("35.90"));

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final InventoryPartDirectory partDirectory =
            new InventoryPartDirectory(partRepository, stockMovementRepository);

    @Test
    void shouldGiveTheSalePriceAndTheAvailableQuantity() {
        Part pad = savedPad(4);
        pad.reserve(1);

        assertThat(partDirectory.detailsOf(pad.getId())).isEqualTo(new PartDetails(SALE_PRICE, 3));
    }

    @Test
    void shouldRefuseWhatTheAvailableStockDoesNotCover() {
        Part pad = savedPad(4);
        pad.reserve(1);

        partDirectory.requireAvailableFor(42L, pad.getId(), 3);
        assertThatThrownBy(() -> partDirectory.requireAvailableFor(42L, pad.getId(), 4))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"PAST-001", 3, 4});
    }

    @Test
    void shouldDiscountWhatAlreadyLeftTheStockForTheServiceOrder() {
        Part pad = savedPad(10);
        stockMovementRepository.save(StockMovement.withdrawal(pad, 2, 42L, 7L));

        partDirectory.requireAvailableFor(42L, pad.getId(), 10);
        assertThatThrownBy(() -> partDirectory.requireAvailableFor(42L, pad.getId(), 11))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"PAST-001", 8, 9});
        assertThatThrownBy(() -> partDirectory.requireAvailableFor(43L, pad.getId(), 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
    }

    @Test
    void shouldReportAnUnknownPart() {
        assertThatThrownBy(() -> partDirectory.detailsOf(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    @Test
    void shouldGiveTheNamesOfTheRequestedParts() {
        Part pad = partRepository.save(
                new Part(new Sku("PAST-001"), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, SALE_PRICE, 2));
        Part oil = partRepository.save(
                new Part(new Sku("OLEO-001"), "Óleo 5W30", PartType.SUPPLY, MeasurementUnit.LITER, SALE_PRICE, 2));
        partRepository.save(
                new Part(new Sku("FILT-001"), "Filtro de ar", PartType.PART, MeasurementUnit.UNIT, SALE_PRICE, 2));

        assertThat(partDirectory.namesOf(List.of(pad.getId(), oil.getId())))
                .containsExactlyInAnyOrderEntriesOf(Map.of(pad.getId(), "Pastilha de freio", oil.getId(), "Óleo 5W30"));
    }

    private Part savedPad(int stockQuantity) {
        Part pad = partRepository.save(
                new Part(new Sku("PAST-001"), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, SALE_PRICE, 2));
        pad.increaseStock(stockQuantity);
        return pad;
    }
}
