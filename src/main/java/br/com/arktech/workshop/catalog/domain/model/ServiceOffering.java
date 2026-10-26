package br.com.arktech.workshop.catalog.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "service_offering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOffering extends BaseEntity {

    private String name;

    private @Nullable String description;

    private Money laborPrice;

    private int estimatedMinutes;

    private boolean active;

    public ServiceOffering(String name, @Nullable String description, Money laborPrice, int estimatedMinutes) {
        requirePositive(laborPrice);
        requirePositive(estimatedMinutes);

        this.name = name;
        this.description = description;
        this.laborPrice = laborPrice;
        this.estimatedMinutes = estimatedMinutes;
        this.active = true;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void describe(@Nullable String description) {
        this.description = description;
    }

    public void changeLaborPrice(Money laborPrice) {
        requirePositive(laborPrice);
        this.laborPrice = laborPrice;
    }

    public void changeEstimatedMinutes(int estimatedMinutes) {
        requirePositive(estimatedMinutes);
        this.estimatedMinutes = estimatedMinutes;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    private static void requirePositive(Money laborPrice) {
        if (!laborPrice.isPositive()) {
            throw new BusinessRuleException("serviceOffering.laborPriceNotPositive");
        }
    }

    private static void requirePositive(int estimatedMinutes) {
        if (estimatedMinutes <= 0) {
            throw new BusinessRuleException("serviceOffering.estimatedMinutesNotPositive");
        }
    }
}
