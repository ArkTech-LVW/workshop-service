package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "service_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItem extends ServiceOrderItem {

    private Long serviceOfferingId;

    private @Nullable Instant completedAt;

    private @Nullable Long completedByUserId;

    ServiceItem(ServiceOrder serviceOrder, Long serviceOfferingId, int quantity, Money unitPrice) {
        super(serviceOrder, quantity, unitPrice);
        this.serviceOfferingId = serviceOfferingId;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    void complete(Long completedByUserId, Instant completedAt) {
        if (isCompleted()) {
            throw new BusinessRuleException("serviceOrder.serviceItemAlreadyCompleted");
        }
        if (!isApproved()) {
            throw new BusinessRuleException("serviceOrder.pendingItemCompletionNotAllowed");
        }
        this.completedAt = completedAt;
        this.completedByUserId = completedByUserId;
    }
}
