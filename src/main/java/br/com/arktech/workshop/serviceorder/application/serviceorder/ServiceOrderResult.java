package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record ServiceOrderResult(
        Long id,
        UUID publicId,
        String number,
        Long customerId,
        Long vehicleId,
        @Nullable Long mechanicId,
        String problemDescription,
        ServiceOrderStatus status,
        List<ServiceItemResult> services,
        List<PartItemResult> parts,
        BigDecimal servicesTotal,
        BigDecimal partsTotal,
        BigDecimal totalPrice,
        BigDecimal approvedTotal,
        BigDecimal pendingTotal,
        @Nullable Boolean canProceedWithoutChanges,
        List<StatusChangeResult> statusHistory,
        @Nullable Instant dateCreated) {

    public ServiceOrderResult {
        services = List.copyOf(services);
        parts = List.copyOf(parts);
        statusHistory = List.copyOf(statusHistory);
    }

    public static ServiceOrderResult from(ServiceOrder serviceOrder) {
        return new ServiceOrderResult(
                serviceOrder.getId(),
                serviceOrder.getPublicId(),
                serviceOrder.getNumber().value(),
                serviceOrder.getCustomerId(),
                serviceOrder.getVehicleId(),
                serviceOrder.getMechanicId(),
                serviceOrder.getProblemDescription(),
                serviceOrder.getStatus(),
                serviceOrder.getServiceItems().stream().map(ServiceItemResult::from).toList(),
                serviceOrder.getPartItems().stream().map(PartItemResult::from).toList(),
                serviceOrder.getServicesTotal().amount(),
                serviceOrder.getPartsTotal().amount(),
                serviceOrder.getTotalPrice().amount(),
                serviceOrder.getApprovedTotal().amount(),
                serviceOrder.getPendingTotal().amount(),
                serviceOrder.getCanProceedWithoutChanges(),
                serviceOrder.getStatusHistory().stream()
                        .map(StatusChangeResult::from)
                        .toList(),
                serviceOrder.getDateCreated());
    }
}
