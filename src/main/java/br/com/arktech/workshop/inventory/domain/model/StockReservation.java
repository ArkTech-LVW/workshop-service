package br.com.arktech.workshop.inventory.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    private Long serviceOrderId;

    private int quantity;

    private Instant expiresAt;

    private StockReservation(Part part, Long serviceOrderId, int quantity, Instant expiresAt) {
        this.part = part;
        this.serviceOrderId = serviceOrderId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public static StockReservation reserve(Part part, Long serviceOrderId, int quantity, Instant expiresAt) {
        part.reserve(quantity);
        return new StockReservation(part, serviceOrderId, quantity, expiresAt);
    }

    public void change(int quantity, Instant expiresAt) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive, got " + quantity);
        }
        if (quantity > this.quantity) {
            part.reserve(quantity - this.quantity);
        } else if (quantity < this.quantity) {
            part.releaseReservation(this.quantity - quantity);
        }

        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public void release() {
        part.releaseReservation(quantity);
    }
}
