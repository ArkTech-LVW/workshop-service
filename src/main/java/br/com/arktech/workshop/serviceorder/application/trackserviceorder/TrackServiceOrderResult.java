package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record TrackServiceOrderResult(
        UUID publicId,
        String number,
        ServiceOrderStatus status,
        String problemDescription,
        VehicleDetails vehicle,
        List<StatusChangeResult> statusHistory,
        @Nullable QuoteResult quote,
        @Nullable Instant dateCreated) {

    public TrackServiceOrderResult {
        statusHistory = List.copyOf(statusHistory);
    }
}
