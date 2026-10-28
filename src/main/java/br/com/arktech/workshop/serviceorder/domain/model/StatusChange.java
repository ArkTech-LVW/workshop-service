package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;

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
@Table(name = "status_change")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusChange extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_order_id")
    private ServiceOrder serviceOrder;

    @Enumerated(EnumType.STRING)
    private @Nullable ServiceOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ServiceOrderStatus toStatus;

    private @Nullable Long changedByUserId;

    private @Nullable String description;

    StatusChange(
            ServiceOrder serviceOrder,
            @Nullable ServiceOrderStatus fromStatus,
            ServiceOrderStatus toStatus,
            @Nullable Long changedByUserId,
            @Nullable String description) {
        this.serviceOrder = serviceOrder;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.description = description;
    }
}
