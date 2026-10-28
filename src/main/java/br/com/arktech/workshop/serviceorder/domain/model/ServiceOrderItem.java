package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ServiceOrderItem extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_order_id")
    private ServiceOrder serviceOrder;

    private int quantity;

    private Money unitPrice;

    protected ServiceOrderItem(ServiceOrder serviceOrder, int quantity, Money unitPrice) {
        requirePositive(quantity);

        this.serviceOrder = serviceOrder;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money getSubtotal() {
        return unitPrice.times(quantity);
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("serviceOrder.itemQuantityNotPositive");
        }
    }
}
