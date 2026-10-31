package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import br.com.arktech.workshop.inventory.application.serviceorderpart.PartLockOrder;
import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveServiceOrderStockUseCase implements VoidUseCase<ReserveServiceOrderStockCommand> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReservationRepository stockReservationRepository;
    private final Clock clock;
    private final Duration validity;

    public ReserveServiceOrderStockUseCase(
            PartRepository partRepository,
            StockMovementRepository stockMovementRepository,
            StockReservationRepository stockReservationRepository,
            Clock clock,
            @Value("${inventory.stock-reservation.validity}") Duration validity) {
        this.partRepository = partRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    @Transactional
    public void execute(ReserveServiceOrderStockCommand command) {
        Map<Long, Integer> requiredByPart = ServiceOrderPart.quantityByPart(command.parts());
        Map<Long, Integer> withdrawnByPart =
                stockMovementRepository.findNetWithdrawnByServiceOrderId(command.serviceOrderId());
        List<Long> reservedPartIds = stockReservationRepository.findPartIdsByServiceOrderId(command.serviceOrderId());
        Instant expiresAt = clock.instant().plus(validity);

        Map<Long, Integer> missingByPart = new HashMap<>();
        for (Long partId : PartLockOrder.of(requiredByPart.keySet(), reservedPartIds)) {
            int missing = Math.max(0, requiredByPart.getOrDefault(partId, 0) - withdrawnByPart.getOrDefault(partId, 0));
            if (missing > 0 || reservedPartIds.contains(partId)) {
                missingByPart.put(partId, missing);
            }
        }

        List<Part> parts = PartLockOrder.lock(missingByPart.keySet(), partRepository);
        Map<Long, StockReservation> reservationByPart = reservationByPart(command.serviceOrderId());
        for (Part part : parts) {
            reserve(
                    part,
                    reservationByPart.get(part.getId()),
                    missingByPart.getOrDefault(part.getId(), 0),
                    command.serviceOrderId(),
                    expiresAt);
        }
    }

    private Map<Long, StockReservation> reservationByPart(Long serviceOrderId) {
        return stockReservationRepository.findByServiceOrderId(serviceOrderId).stream()
                .collect(Collectors.toMap(reservation -> reservation.getPart().getId(), reservation -> reservation));
    }

    private void reserve(
            Part part, @Nullable StockReservation current, int quantity, Long serviceOrderId, Instant expiresAt) {
        if (current != null && quantity == 0) {
            current.release();
            stockReservationRepository.delete(current);
        } else if (current != null) {
            current.change(quantity, expiresAt);
            stockReservationRepository.save(current);
        } else if (quantity > 0) {
            stockReservationRepository.save(StockReservation.reserve(part, serviceOrderId, quantity, expiresAt));
        }
        partRepository.save(part);
    }
}
