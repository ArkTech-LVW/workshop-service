package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StockReservationTest {

    private static final Long SERVICE_ORDER_ID = 42L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-07T12:00:00Z");

    private Part part;

    @BeforeEach
    void setUp() {
        part = new Part(
                new Sku("FILTRO-01"), "Filtro de óleo", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1);
        part.increaseStock(5);
    }

    @Test
    void shouldReserveThePartForTheServiceOrder() {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);

        assertThat(reservation.getPart()).isSameAs(part);
        assertThat(reservation.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
        assertThat(reservation.getQuantity()).isEqualTo(3);
        assertThat(reservation.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(part.getReservedQuantity()).isEqualTo(3);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRefuseToReserveMoreThanIsAvailable() {
        assertThatThrownBy(() -> StockReservation.reserve(part, SERVICE_ORDER_ID, 6, EXPIRES_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(part.getReservedQuantity()).isZero();
    }

    @Test
    void shouldReserveOnlyTheDifferenceWhenTheQuantityChanges() {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);
        Instant renewed = EXPIRES_AT.plusSeconds(60);

        reservation.change(5, renewed);
        assertThat(part.getReservedQuantity()).isEqualTo(5);

        reservation.change(1, renewed);
        assertThat(part.getReservedQuantity()).isEqualTo(1);
        assertThat(reservation.getQuantity()).isEqualTo(1);
        assertThat(reservation.getExpiresAt()).isEqualTo(renewed);
    }

    @Test
    void shouldOnlyRenewTheExpirationWhenTheQuantityIsTheSame() {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);
        Instant renewed = EXPIRES_AT.plusSeconds(60);

        reservation.change(3, renewed);

        assertThat(part.getReservedQuantity()).isEqualTo(3);
        assertThat(reservation.getExpiresAt()).isEqualTo(renewed);
    }

    @Test
    void shouldRefuseToGrowBeyondTheAvailableStock() {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);

        assertThatThrownBy(() -> reservation.change(6, EXPIRES_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(reservation.getQuantity()).isEqualTo(3);
        assertThat(part.getReservedQuantity()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRefuseToChangeToANonPositiveQuantity(int quantity) {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);

        assertThatThrownBy(() -> reservation.change(quantity, EXPIRES_AT)).isInstanceOf(IllegalArgumentException.class);
        assertThat(reservation.getQuantity()).isEqualTo(3);
        assertThat(part.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void shouldGiveTheReservedQuantityBackToTheAvailableStock() {
        StockReservation reservation = StockReservation.reserve(part, SERVICE_ORDER_ID, 3, EXPIRES_AT);

        reservation.release();

        assertThat(part.getReservedQuantity()).isZero();
        assertThat(part.getAvailableQuantity()).isEqualTo(5);
    }
}
