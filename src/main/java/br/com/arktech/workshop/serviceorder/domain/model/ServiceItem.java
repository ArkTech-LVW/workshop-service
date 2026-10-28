package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItem extends ServiceOrderItem {

    private Long serviceOfferingId;

    ServiceItem(ServiceOrder serviceOrder, Long serviceOfferingId, int quantity, Money unitPrice) {
        super(serviceOrder, quantity, unitPrice);
        this.serviceOfferingId = serviceOfferingId;
    }
}
