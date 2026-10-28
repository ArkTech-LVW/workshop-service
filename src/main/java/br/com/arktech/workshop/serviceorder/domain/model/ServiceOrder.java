package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteApproved;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteRejected;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteReopened;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteSent;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.ServiceOrderCanceled;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.ServiceOrderCompleted;
import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "service_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOrder extends BaseEntity {

    private static final Money MAX_TOTAL_PRICE = new Money(new BigDecimal("9999999999.99"));

    private UUID publicId;

    private ServiceOrderNumber number;

    private Long customerId;

    private Long vehicleId;

    private @Nullable Long mechanicId;

    private String problemDescription;

    @Enumerated(EnumType.STRING)
    private ServiceOrderStatus status;

    private Money totalPrice;

    @Getter(AccessLevel.NONE)
    @BatchSize(size = 100)
    @OrderBy("id")
    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceItem> serviceItems = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @BatchSize(size = 100)
    @OrderBy("id")
    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartItem> partItems = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @BatchSize(size = 100)
    @OrderBy("id")
    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusChange> statusHistory = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private List<ServiceOrderEvent> events = new ArrayList<>();

    public ServiceOrder(
            ServiceOrderNumber number,
            Long customerId,
            Long vehicleId,
            String problemDescription,
            Long openedByUserId) {
        this.publicId = UUID.randomUUID();
        this.number = number;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.problemDescription = problemDescription;
        this.status = ServiceOrderStatus.RECEIVED;
        this.totalPrice = Money.ZERO;
        statusHistory.add(new StatusChange(this, null, ServiceOrderStatus.RECEIVED, openedByUserId, null));
    }

    public List<ServiceItem> getServiceItems() {
        return List.copyOf(serviceItems);
    }

    public List<PartItem> getPartItems() {
        return List.copyOf(partItems);
    }

    public List<StatusChange> getStatusHistory() {
        return List.copyOf(statusHistory);
    }

    public List<ServiceOrderEvent> pullEvents() {
        List<ServiceOrderEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public void addServiceItem(Long serviceOfferingId, int quantity, Money unitPrice) {
        requireItemChangesAllowed();
        ServiceItem item = new ServiceItem(this, serviceOfferingId, quantity, unitPrice);
        requireTotalPriceWithinLimit(item);
        serviceItems.add(item);
        recalculateTotalPrice();
    }

    public void addPartItem(Long partId, int quantity, Money unitPrice) {
        requireItemChangesAllowed();
        PartItem item = new PartItem(this, partId, quantity, unitPrice);
        requireTotalPriceWithinLimit(item);
        partItems.add(item);
        recalculateTotalPrice();
    }

    public void removeServiceItem(Long itemId) {
        removeItem(serviceItems, itemId);
    }

    public void removePartItem(Long itemId) {
        removeItem(partItems, itemId);
    }

    public void startDiagnosis(Long mechanicId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.IN_DIAGNOSIS);
        this.mechanicId = mechanicId;
        moveTo(ServiceOrderStatus.IN_DIAGNOSIS, mechanicId, description);
    }

    public void sendQuote(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.AWAITING_APPROVAL);
        if (serviceItems.isEmpty() && partItems.isEmpty()) {
            throw new BusinessRuleException("serviceOrder.quoteWithoutItems");
        }
        moveTo(ServiceOrderStatus.AWAITING_APPROVAL, changedByUserId, description);
        events.add(new QuoteSent(publicId));
    }

    public void approveQuote(@Nullable Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL, ServiceOrderStatus.QUOTE_APPROVED);
        moveTo(ServiceOrderStatus.QUOTE_APPROVED, changedByUserId, description);
        events.add(new QuoteApproved(publicId));
    }

    public void rejectQuote(@Nullable Long changedByUserId, String reason) {
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL, ServiceOrderStatus.QUOTE_REJECTED);
        requireReason(reason);
        moveTo(ServiceOrderStatus.QUOTE_REJECTED, changedByUserId, reason);
        events.add(new QuoteRejected(publicId));
    }

    public void reopenQuote(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.QUOTE_REJECTED, ServiceOrderStatus.IN_DIAGNOSIS);
        moveTo(ServiceOrderStatus.IN_DIAGNOSIS, changedByUserId, description);
        events.add(new QuoteReopened(publicId));
    }

    public void startExecution(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.QUOTE_APPROVED, ServiceOrderStatus.IN_PROGRESS);
        moveTo(ServiceOrderStatus.IN_PROGRESS, changedByUserId, description);
    }

    public void complete(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.COMPLETED);
        moveTo(ServiceOrderStatus.COMPLETED, changedByUserId, description);
        events.add(new ServiceOrderCompleted(publicId));
    }

    public void deliver(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.COMPLETED, ServiceOrderStatus.DELIVERED);
        moveTo(ServiceOrderStatus.DELIVERED, changedByUserId, description);
    }

    public void cancel(Long changedByUserId, String reason) {
        if (!status.allowsCancellation()) {
            throw new InvalidStatusTransitionException(status, ServiceOrderStatus.CANCELED);
        }
        requireReason(reason);
        moveTo(ServiceOrderStatus.CANCELED, changedByUserId, reason);
        events.add(new ServiceOrderCanceled(publicId));
    }

    private void requireStatus(ServiceOrderStatus expected, ServiceOrderStatus next) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(status, next);
        }
    }

    private static void requireReason(String reason) {
        if (reason.isBlank()) {
            throw new BusinessRuleException("serviceOrder.reasonRequired");
        }
    }

    private void moveTo(ServiceOrderStatus next, @Nullable Long changedByUserId, @Nullable String description) {
        statusHistory.add(new StatusChange(this, status, next, changedByUserId, description));
        status = next;
    }

    private void removeItem(List<? extends ServiceOrderItem> items, Long itemId) {
        requireItemChangesAllowed();
        if (!items.removeIf(item -> itemId.equals(item.getId()))) {
            throw new NotFoundException("serviceOrder.itemNotFound", itemId.toString());
        }
        recalculateTotalPrice();
    }

    private void requireItemChangesAllowed() {
        if (!status.allowsItemChanges()) {
            throw new BusinessRuleException("serviceOrder.itemsLocked");
        }
    }

    private void requireTotalPriceWithinLimit(ServiceOrderItem newItem) {
        if (totalPrice.plus(newItem.getSubtotal()).isGreaterThan(MAX_TOTAL_PRICE)) {
            throw new BusinessRuleException("serviceOrder.totalPriceTooHigh");
        }
    }

    private void recalculateTotalPrice() {
        totalPrice = Stream.concat(serviceItems.stream(), partItems.stream())
                .map(ServiceOrderItem::getSubtotal)
                .reduce(Money.ZERO, Money::plus);
    }
}
