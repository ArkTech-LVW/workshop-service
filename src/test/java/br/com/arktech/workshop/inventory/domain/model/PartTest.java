package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PartTest {

    private static final Sku SKU = new Sku("OLEO-5W30-1L");
    private static final Money PRICE = new Money(new BigDecimal("45.90"));

    @Test
    void shouldStartWithEmptyStockAndGivenData() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);

        assertThat(part.getSku()).isEqualTo(SKU);
        assertThat(part.getName()).isEqualTo("Óleo 5W30 1L");
        assertThat(part.getType()).isEqualTo(PartType.SUPPLY);
        assertThat(part.getUnit()).isEqualTo(MeasurementUnit.LITER);
        assertThat(part.getSalePrice()).isEqualTo(PRICE);
        assertThat(part.getStockQuantity()).isZero();
        assertThat(part.getMinimumStock()).isEqualTo(10);
    }

    @Test
    void shouldAcceptZeroSalePriceAndMinimumStock() {
        Part part = new Part(SKU, "Brinde", PartType.PART, MeasurementUnit.UNIT, Money.ZERO, 0);

        assertThat(part.getSalePrice()).isEqualTo(Money.ZERO);
        assertThat(part.getMinimumStock()).isZero();
    }

    @Test
    void shouldRejectNegativeMinimumStock() {
        assertThatThrownBy(() -> new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, -1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
    }
}
