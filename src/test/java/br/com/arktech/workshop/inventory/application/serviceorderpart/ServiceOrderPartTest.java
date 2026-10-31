package br.com.arktech.workshop.inventory.application.serviceorderpart;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ServiceOrderPartTest {

    @Test
    void shouldSumTheQuantityOfEachPart() {
        assertThat(ServiceOrderPart.quantityByPart(List.of(
                        new ServiceOrderPart(2L, 4), new ServiceOrderPart(1L, 1), new ServiceOrderPart(2L, 2))))
                .isEqualTo(Map.of(1L, 1, 2L, 6));
    }

    @Test
    void shouldLockTheGivenPartsInIdOrder() {
        InMemoryPartRepository partRepository = new InMemoryPartRepository();
        Part first = savePart(partRepository, "PECA-1");
        Part second = savePart(partRepository, "PECA-2");
        Part third = savePart(partRepository, "PECA-3");

        assertThat(PartLockOrder.lock(Set.of(third.getId(), first.getId(), second.getId()), partRepository))
                .containsExactly(first, second, third);
    }

    @Test
    void shouldLockEachPartOnceInIdOrder() {
        assertThat(PartLockOrder.of(Set.of(3L, 1L), List.of(2L, 3L), List.of()))
                .containsExactly(1L, 2L, 3L);
    }

    private static Part savePart(InMemoryPartRepository partRepository, String sku) {
        return partRepository.save(new Part(
                new Sku(sku), "Item " + sku, PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
    }
}
