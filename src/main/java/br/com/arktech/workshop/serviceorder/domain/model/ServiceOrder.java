package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.event.serviceorder.PartQuantity;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteWithdrawnEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCanceledEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

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

    private @Nullable Boolean canProceedWithoutChanges;

    private boolean approvedItemRemoved;

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
    private List<Object> events = new ArrayList<>();

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
        this.problemDescription = requireText(problemDescription, "serviceOrder.problemDescriptionRequired");
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

    public Money getServicesTotal() {
        return sumSubtotals(serviceItems);
    }

    public Money getPartsTotal() {
        return sumSubtotals(partItems);
    }

    public Money getApprovedTotal() {
        return sumSubtotals(items().filter(ServiceOrderItem::isApproved).toList());
    }

    public Money getPendingTotal() {
        return sumSubtotals(items().filter(item -> !item.isApproved()).toList());
    }

    public List<StatusChange> getStatusHistory() {
        return List.copyOf(statusHistory);
    }

    @DomainEvents
    public List<Object> domainEvents() {
        return List.copyOf(events);
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        events.clear();
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

    public void sendQuote(
            Long changedByUserId, @Nullable Boolean canProceedWithoutChanges, @Nullable String description) {
        if (status.allowsSupplementaryQuote()) {
            sendSupplementaryQuote(changedByUserId, canProceedWithoutChanges, description);
            return;
        }
        requireStatus(ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.AWAITING_APPROVAL);
        if (canProceedWithoutChanges != null) {
            throw new BusinessRuleException("serviceOrder.canProceedWithoutChangesNotAllowed");
        }
        requireItems();
        moveTo(ServiceOrderStatus.AWAITING_APPROVAL, changedByUserId, description);
        events.add(new QuoteSentEvent(getId(), publicId, customerId, totalPrice.amount(), partQuantities()));
    }

    public void approveQuote(@Nullable Long changedByUserId, @Nullable String description, Instant approvedAt) {
        ServiceOrderStatus next = switch (status) {
            case AWAITING_APPROVAL -> ServiceOrderStatus.QUOTE_APPROVED;
            case AWAITING_SUPPLEMENTARY_APPROVAL -> ServiceOrderStatus.SUPPLEMENTARY_APPROVED;
            default -> throw new InvalidStatusTransitionException(status, ServiceOrderStatus.QUOTE_APPROVED);
        };
        items().filter(item -> !item.isApproved()).forEach(item -> item.approve(approvedAt));
        concludeApproval(next, changedByUserId, description);
    }

    public void approveOriginalOnly(@Nullable Long changedByUserId, @Nullable String description) {
        requireStatus(
                ServiceOrderStatus.AWAITING_SUPPLEMENTARY_APPROVAL, ServiceOrderStatus.SUPPLEMENTARY_APPROVED);
        if (!Boolean.TRUE.equals(canProceedWithoutChanges)) {
            throw new BusinessRuleException("serviceOrder.originalOnlyNotAllowed");
        }
        serviceItems.removeIf(item -> !item.isApproved());
        partItems.removeIf(item -> !item.isApproved());
        recalculateTotalPrice();
        concludeApproval(ServiceOrderStatus.SUPPLEMENTARY_APPROVED, changedByUserId, description);
    }

    public void rejectQuote(@Nullable Long changedByUserId, String reason) {
        ServiceOrderStatus next = switch (status) {
            case AWAITING_APPROVAL -> ServiceOrderStatus.QUOTE_REJECTED;
            case AWAITING_SUPPLEMENTARY_APPROVAL -> ServiceOrderStatus.SUPPLEMENTARY_REJECTED;
            default -> throw new InvalidStatusTransitionException(status, ServiceOrderStatus.QUOTE_REJECTED);
        };
        requireText(reason, "serviceOrder.reasonRequired");
        canProceedWithoutChanges = null;
        moveTo(next, changedByUserId, reason);
        events.add(new QuoteRejectedEvent(getId(), publicId, customerId, totalPrice.amount(), reason));
    }

    public void reopenQuote(Long changedByUserId, @Nullable String description) {
        if (status == ServiceOrderStatus.QUOTE_REJECTED) {
            moveTo(ServiceOrderStatus.IN_DIAGNOSIS, changedByUserId, description);
            return;
        }
        ServiceOrderStatus next = switch (status) {
            case AWAITING_APPROVAL -> ServiceOrderStatus.IN_DIAGNOSIS;
            case AWAITING_SUPPLEMENTARY_APPROVAL -> ServiceOrderStatus.IN_PROGRESS;
            default -> throw new InvalidStatusTransitionException(status, ServiceOrderStatus.IN_DIAGNOSIS);
        };
        canProceedWithoutChanges = null;
        moveTo(next, changedByUserId, description);
        events.add(new QuoteWithdrawnEvent(getId()));
    }

    public void startExecution(Long changedByUserId, @Nullable String description) {
        if (status != ServiceOrderStatus.QUOTE_APPROVED && status != ServiceOrderStatus.SUPPLEMENTARY_APPROVED) {
            throw new InvalidStatusTransitionException(status, ServiceOrderStatus.IN_PROGRESS);
        }
        moveTo(ServiceOrderStatus.IN_PROGRESS, changedByUserId, description);
    }

    public void completeServiceItem(Long itemId, Long completedByUserId, Instant completedAt) {
        if (status != ServiceOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException("serviceOrder.serviceItemCompletionNotAllowed");
        }
        ServiceItem item = serviceItems.stream()
                .filter(serviceItem -> itemId.equals(serviceItem.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("serviceOrder.itemNotFound", itemId.toString()));
        item.complete(completedByUserId, completedAt);
        if (serviceItems.stream().allMatch(ServiceItem::isCompleted) && !hasPendingChanges()) {
            moveToCompleted(completedByUserId, null);
        }
    }

    public void complete(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.COMPLETED);
        if (!serviceItems.stream().allMatch(ServiceItem::isCompleted)) {
            throw new BusinessRuleException("serviceOrder.pendingServiceItems");
        }
        if (hasPendingChanges()) {
            throw new BusinessRuleException("serviceOrder.quoteChangesPendingApproval");
        }
        moveToCompleted(changedByUserId, description);
    }

    public void deliver(Long changedByUserId, @Nullable String description) {
        requireStatus(ServiceOrderStatus.COMPLETED, ServiceOrderStatus.DELIVERED);
        moveTo(ServiceOrderStatus.DELIVERED, changedByUserId, description);
    }

    public void cancel(Long changedByUserId, String reason) {
        if (!status.allowsCancellation()) {
            throw new InvalidStatusTransitionException(status, ServiceOrderStatus.CANCELED);
        }
        requireText(reason, "serviceOrder.reasonRequired");
        moveTo(ServiceOrderStatus.CANCELED, changedByUserId, reason);
        events.add(new ServiceOrderCanceledEvent(getId(), publicId, changedByUserId));
    }

    private void sendSupplementaryQuote(
            Long changedByUserId, @Nullable Boolean canProceedWithoutChanges, @Nullable String description) {
        if (status == ServiceOrderStatus.IN_PROGRESS && !hasPendingChanges()) {
            throw new BusinessRuleException("serviceOrder.noQuoteChanges");
        }
        requireItems();
        if (canProceedWithoutChanges == null) {
            throw new BusinessRuleException("serviceOrder.canProceedWithoutChangesRequired");
        }
        if (canProceedWithoutChanges && approvedItemRemoved) {
            throw new BusinessRuleException("serviceOrder.originalOnlyNotPossible");
        }
        this.canProceedWithoutChanges = canProceedWithoutChanges;
        moveTo(ServiceOrderStatus.AWAITING_SUPPLEMENTARY_APPROVAL, changedByUserId, description);
        events.add(new QuoteSentEvent(getId(), publicId, customerId, totalPrice.amount(), partQuantities()));
    }

    private void concludeApproval(
            ServiceOrderStatus next, @Nullable Long changedByUserId, @Nullable String description) {
        canProceedWithoutChanges = null;
        approvedItemRemoved = false;
        moveTo(next, changedByUserId, description);
        events.add(new QuoteApprovedEvent(
                getId(),
                publicId,
                customerId,
                totalPrice.amount(),
                partQuantities(),
                changedByUserId));
    }

    private List<PartQuantity> partQuantities() {
        return partItems.stream()
                .map(item -> new PartQuantity(item.getPartId(), item.getQuantity()))
                .toList();
    }

    private boolean hasPendingChanges() {
        return approvedItemRemoved || items().anyMatch(item -> !item.isApproved());
    }

    private Stream<ServiceOrderItem> items() {
        return Stream.concat(serviceItems.stream(), partItems.stream());
    }

    private void requireItems() {
        if (serviceItems.isEmpty() && partItems.isEmpty()) {
            throw new BusinessRuleException("serviceOrder.quoteWithoutItems");
        }
    }

    private void requireStatus(ServiceOrderStatus expected, ServiceOrderStatus next) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(status, next);
        }
    }

    private static String requireText(@Nullable String text, String code) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException(code);
        }
        return text;
    }

    private void moveTo(ServiceOrderStatus next, @Nullable Long changedByUserId, @Nullable String description) {
        statusHistory.add(new StatusChange(this, status, next, changedByUserId, description));
        status = next;
    }

    private void moveToCompleted(Long changedByUserId, @Nullable String description) {
        moveTo(ServiceOrderStatus.COMPLETED, changedByUserId, description);
        events.add(new ServiceOrderCompletedEvent(getId(), publicId, customerId, totalPrice.amount()));
    }

    private void removeItem(List<? extends ServiceOrderItem> items, Long itemId) {
        requireItemChangesAllowed();
        ServiceOrderItem item = items.stream()
                .filter(candidate -> itemId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("serviceOrder.itemNotFound", itemId.toString()));
        if (item instanceof ServiceItem serviceItem && serviceItem.isCompleted()) {
            throw new BusinessRuleException("serviceOrder.completedItemLocked");
        }
        items.remove(item);
        if (item.isApproved()) {
            approvedItemRemoved = true;
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
        totalPrice = getServicesTotal().plus(getPartsTotal());
    }

    private static Money sumSubtotals(List<? extends ServiceOrderItem> items) {
        return items.stream().map(ServiceOrderItem::getSubtotal).reduce(Money.ZERO, Money::plus);
    }
}
