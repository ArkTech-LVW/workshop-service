package br.com.arktech.workshop.inventory.application.serviceorderpart;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldLockEachPartOnceInIdOrder() {
        assertThat(PartLockOrder.of(Set.of(3L, 1L), List.of(2L, 3L), List.of()))
                .containsExactly(1L, 2L, 3L);
    }
}
