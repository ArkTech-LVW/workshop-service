package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "part_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartItem extends ServiceOrderItem {

    private Long partId;

    PartItem(ServiceOrder serviceOrder, Long partId, int quantity, Money unitPrice) {
        super(serviceOrder, quantity, unitPrice);
        this.partId = partId;
    }
}
