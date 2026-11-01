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

    private @Nullable Instant startedAt;

    private @Nullable Long startedByUserId;

    private @Nullable Instant completedAt;

    private @Nullable Long completedByUserId;

    ServiceItem(ServiceOrder serviceOrder, Long serviceOfferingId, int quantity, Money unitPrice) {
        super(serviceOrder, quantity, unitPrice);
        this.serviceOfferingId = serviceOfferingId;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    void start(Long startedByUserId, Instant startedAt) {
        if (isStarted()) {
            throw new BusinessRuleException("serviceOrder.serviceItemAlreadyStarted");
        }
        if (!isApproved()) {
            throw new BusinessRuleException("serviceOrder.pendingItemStartNotAllowed");
        }
        this.startedAt = startedAt;
        this.startedByUserId = startedByUserId;
    }

    void complete(Long completedByUserId, Instant completedAt) {
        if (isCompleted()) {
            throw new BusinessRuleException("serviceOrder.serviceItemAlreadyCompleted");
        }
        if (!isApproved()) {
            throw new BusinessRuleException("serviceOrder.pendingItemCompletionNotAllowed");
        }
        if (!isStarted()) {
            throw new BusinessRuleException("serviceOrder.serviceItemNotStarted");
        }
        this.completedAt = completedAt;
        this.completedByUserId = completedByUserId;
    }
}
