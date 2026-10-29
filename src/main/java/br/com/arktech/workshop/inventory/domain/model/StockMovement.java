package br.com.arktech.workshop.inventory.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "stock_movement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    private @Nullable Long serviceOrderId;

    @Enumerated(EnumType.STRING)
    private StockMovementType type;

    private int quantity;

    private @Nullable String reason;

    private @Nullable Long createdByUserId;

    private StockMovement(
            Part part,
            StockMovementType type,
            int quantity,
            @Nullable String reason,
            @Nullable Long serviceOrderId,
            @Nullable Long userId) {
        this.part = part;
        this.serviceOrderId = serviceOrderId;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.createdByUserId = userId;
    }

    public static StockMovement entry(Part part, int quantity, @Nullable String reason, Long userId) {
        part.increaseStock(quantity);
        return new StockMovement(part, StockMovementType.IN, quantity, normalize(reason), null, userId);
    }

    public static StockMovement adjustment(Part part, int quantity, @Nullable String reason, Long userId) {
        String normalized = normalize(reason);
        if (normalized == null) {
            throw new BusinessRuleException("stockMovement.reasonRequired");
        }

        part.decreaseStock(quantity);
        return new StockMovement(part, StockMovementType.ADJUSTMENT, quantity, normalized, null, userId);
    }

    public static StockMovement withdrawal(Part part, int quantity, Long serviceOrderId, @Nullable Long userId) {
        part.decreaseStock(quantity);
        return new StockMovement(part, StockMovementType.OUT, quantity, null, serviceOrderId, userId);
    }

    private static @Nullable String normalize(@Nullable String reason) {
        return reason == null || reason.isBlank() ? null : reason.strip();
    }
}
