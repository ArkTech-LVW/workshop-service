package br.com.arktech.workshop.inventory.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "part")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends BaseEntity {

    private Sku sku;

    private String name;

    @Enumerated(EnumType.STRING)
    private PartType type;

    @Enumerated(EnumType.STRING)
    private MeasurementUnit unit;

    private Money salePrice;

    private int stockQuantity;

    private int minimumStock;

    public Part(Sku sku, String name, PartType type, MeasurementUnit unit, Money salePrice, int minimumStock) {
        requireNonNegative(minimumStock);

        this.sku = sku;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.salePrice = salePrice;
        this.stockQuantity = 0;
        this.minimumStock = minimumStock;
    }

    private static void requireNonNegative(int minimumStock) {
        if (minimumStock < 0) {
            throw new BusinessRuleException("part.minimumStockNegative");
        }
    }
}
