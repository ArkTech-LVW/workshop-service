package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertThat(part.getReservedQuantity()).isZero();
        assertThat(part.getAvailableQuantity()).isZero();
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
    void shouldIncreaseAndDecreaseStock() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);

        part.increaseStock(8);
        part.decreaseStock(3);

        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldAllowDecreasingTheWholeStock() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);
        part.increaseStock(4);

        part.decreaseStock(4);

        assertThat(part.getStockQuantity()).isZero();
    }

    @Test
    void shouldRejectDecreaseGreaterThanStock() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);
        part.increaseStock(2);

        assertThatThrownBy(() -> part.decreaseStock(3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"OLEO-5W30-1L", 2, 3});
        assertThat(part.getStockQuantity()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveStockQuantity(int quantity) {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 10);

        assertThatThrownBy(() -> part.increaseStock(quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.quantityNotPositive");
        assertThatThrownBy(() -> part.decreaseStock(quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.quantityNotPositive");
        assertThatThrownBy(() -> part.reserve(quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.quantityNotPositive");
        assertThatThrownBy(() -> part.releaseReservation(quantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.quantityNotPositive");
    }

    @Test
    void shouldBeLowStockUpToTheMinimum() {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 2);

        part.increaseStock(2);
        assertThat(part.isLowStock()).isTrue();
        part.increaseStock(1);
        assertThat(part.isLowStock()).isFalse();
    }

    @Test
    void shouldRejectNegativeMinimumStock() {
        assertThatThrownBy(() -> new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, -1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.minimumStockNegative");
    }

    @Test
    void shouldReserveAndReleaseWithoutTouchingTheStock() {
        Part part = stocked(5);

        part.reserve(3);
        assertThat(part.getReservedQuantity()).isEqualTo(3);
        assertThat(part.getAvailableQuantity()).isEqualTo(2);

        part.releaseReservation(1);
        assertThat(part.getReservedQuantity()).isEqualTo(2);
        assertThat(part.getAvailableQuantity()).isEqualTo(3);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectReservingMoreThanTheAvailableStock() {
        Part part = stocked(5);
        part.reserve(3);

        assertThatThrownBy(() -> part.reserve(3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"OLEO-5W30-1L", 2, 3});
        assertThat(part.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void shouldNotDecreaseTheReservedStock() {
        Part part = stocked(5);
        part.reserve(3);

        assertThatThrownBy(() -> part.decreaseStock(3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"OLEO-5W30-1L", 2, 3});

        part.decreaseStock(2);
        assertThat(part.getStockQuantity()).isEqualTo(3);
        assertThat(part.getAvailableQuantity()).isZero();
    }

    @Test
    void shouldRejectReleasingMoreThanIsReserved() {
        Part part = stocked(5);
        part.reserve(2);

        assertThatThrownBy(() -> part.releaseReservation(3)).isInstanceOf(IllegalStateException.class);
        assertThat(part.getReservedQuantity()).isEqualTo(2);
    }

    @Test
    void shouldRequireOnlyWhatTheAvailableStockCovers() {
        Part part = stocked(5);
        part.reserve(2);

        part.requireAvailable(3);
        part.requireAvailable(0);
        part.requireAvailable(-2);
        assertThatThrownBy(() -> part.requireAvailable(4))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock")
                .extracting(error -> ((BusinessRuleException) error).getArgs())
                .isEqualTo(new Object[] {"OLEO-5W30-1L", 3, 4});
    }

    @Test
    void shouldBeLowStockByTheAvailableQuantity() {
        Part part = stocked(5);
        part.changeMinimumStock(2);
        assertThat(part.isLowStock()).isFalse();

        part.reserve(3);
        assertThat(part.isLowStock()).isTrue();
    }

    private static Part stocked(int quantity) {
        Part part = new Part(SKU, "Óleo 5W30 1L", PartType.SUPPLY, MeasurementUnit.LITER, PRICE, 0);
        part.increaseStock(quantity);
        return part;
    }
}
