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
    void shouldChangeDataWithoutTouchingStockQuantity() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);
        Money newPrice = new Money(new BigDecimal("49.90"));

        part.changeSku(new Sku("OLEO-5W40-1L"));
        part.rename("Óleo 5W40 1L");
        part.classify(PartType.PART, MeasurementUnit.UNIT);
        part.changeSalePrice(newPrice);
        part.changeMinimumStock(0);

        assertThat(part.getSku()).isEqualTo(new Sku("OLEO-5W40-1L"));
        assertThat(part.getName()).isEqualTo("Óleo 5W40 1L");
        assertThat(part.getType()).isEqualTo(PartType.PART);
        assertThat(part.getUnit()).isEqualTo(MeasurementUnit.UNIT);
        assertThat(part.getSalePrice()).isEqualTo(newPrice);
        assertThat(part.getMinimumStock()).isZero();
        assertThat(part.getStockQuantity()).isZero();
    }

    @Test
    void shouldRejectChangeToNegativeMinimumStock() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);

        assertThatThrownBy(() -> part.changeMinimumStock(-1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
        assertThat(part.getMinimumStock()).isEqualTo(10);
    }

    @Test
    void shouldRejectNegativeMinimumStock() {
        assertThatThrownBy(() -> new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, -1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
    }
}
