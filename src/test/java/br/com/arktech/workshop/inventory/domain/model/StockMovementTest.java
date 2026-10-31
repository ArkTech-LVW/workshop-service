package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class StockMovementTest {

    private static final Long USER_ID = 7L;

    private Part part;

    @BeforeEach
    void setUp() {
        part = new Part(
                new Sku("FILTRO-01"), "Filtro de óleo", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1);
        ReflectionTestUtils.setField(part, "id", 3L);
        part.increaseStock(5);
    }

    @Test
    void shouldRecordEntryAndIncreaseStock() {
        StockMovement movement = StockMovement.entry(part, 4, "  NF 1234 ", USER_ID);

        assertThat(part.getStockQuantity()).isEqualTo(9);
        assertThat(movement.getPart()).isSameAs(part);
        assertThat(movement.getType()).isEqualTo(StockMovementType.IN);
        assertThat(movement.getQuantity()).isEqualTo(4);
        assertThat(movement.getReason()).isEqualTo("NF 1234");
        assertThat(movement.getServiceOrderId()).isNull();
        assertThat(movement.getCreatedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldAcceptEntryWithoutReason() {
        StockMovement movement = StockMovement.entry(part, 1, " ", USER_ID);

        assertThat(movement.getReason()).isNull();
    }

    @Test
    void shouldRecordAdjustmentAndDecreaseStock() {
        StockMovement movement = StockMovement.adjustment(part, 2, "Avaria no estoque", USER_ID);

        assertThat(part.getStockQuantity()).isEqualTo(3);
        assertThat(movement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(movement.getReason()).isEqualTo("Avaria no estoque");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRequireReasonOnAdjustment(String reason) {
        assertThatThrownBy(() -> StockMovement.adjustment(part, 1, reason, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.reasonRequired");
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRecordWithdrawalForServiceOrder() {
        StockMovement movement = StockMovement.withdrawal(part, 5, 42L, USER_ID);

        assertThat(part.getStockQuantity()).isZero();
        assertThat(movement.getType()).isEqualTo(StockMovementType.OUT);
        assertThat(movement.getServiceOrderId()).isEqualTo(42L);
        assertThat(movement.getReason()).isNull();
    }

    @Test
    void shouldRecordTheReturnOfAPartFromAServiceOrderWithoutUser() {
        StockMovement movement = StockMovement.serviceOrderReturn(part, 3, 42L, null);

        assertThat(part.getStockQuantity()).isEqualTo(8);
        assertThat(movement.getType()).isEqualTo(StockMovementType.IN);
        assertThat(movement.getQuantity()).isEqualTo(3);
        assertThat(movement.getServiceOrderId()).isEqualTo(42L);
        assertThat(movement.getCreatedByUserId()).isNull();
    }

    @Test
    void shouldRejectMovementsThatWouldMakeStockNegative() {
        assertThatThrownBy(() -> StockMovement.withdrawal(part, 6, 42L, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThatThrownBy(() -> StockMovement.adjustment(part, 6, "Contagem", USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }
}
