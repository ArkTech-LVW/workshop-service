package br.com.arktech.workshop.serviceorder.application.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
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
        BigDecimal totalPrice,
        @Nullable Instant dateCreated) {

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
                serviceOrder.getTotalPrice().amount(),
                serviceOrder.getDateCreated());
    }
}
