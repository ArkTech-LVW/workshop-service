package br.com.arktech.workshop.inventory.application.part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InventoryPartDirectoryTest {

    private static final Money SALE_PRICE = new Money(new BigDecimal("35.90"));

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InventoryPartDirectory partDirectory = new InventoryPartDirectory(partRepository);

    @Test
    void shouldGiveTheSalePriceAndTheQuantityInStock() {
        Part pad = partRepository.save(
                new Part(new Sku("PAST-001"), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, SALE_PRICE, 2));
        pad.increaseStock(4);

        assertThat(partDirectory.detailsOf(pad.getId())).isEqualTo(new PartDetails(SALE_PRICE, 4));
    }

    @Test
    void shouldReportAnUnknownPart() {
        assertThatThrownBy(() -> partDirectory.detailsOf(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }
}
