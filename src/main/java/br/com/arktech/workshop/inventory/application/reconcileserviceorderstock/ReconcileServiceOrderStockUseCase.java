package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.inventory.application.releaseserviceorderreservation.ReleaseServiceOrderReservationCommand;
import br.com.arktech.workshop.inventory.application.releaseserviceorderreservation.ReleaseServiceOrderReservationUseCase;
import br.com.arktech.workshop.inventory.application.serviceorderpart.PartLockOrder;
import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconcileServiceOrderStockUseCase implements VoidUseCase<ReconcileServiceOrderStockCommand> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ReleaseServiceOrderReservationUseCase releaseServiceOrderReservationUseCase;

    @Override
    @Transactional
    public void execute(ReconcileServiceOrderStockCommand command) {
        Map<Long, Integer> requiredByPart = ServiceOrderPart.quantityByPart(command.parts());
        Map<Long, Integer> withdrawnByPart =
                stockMovementRepository.findNetWithdrawnByServiceOrderId(command.serviceOrderId());
        List<Long> reservedPartIds = stockReservationRepository.findPartIdsByServiceOrderId(command.serviceOrderId());

        Map<Long, Integer> differenceByPart = new HashMap<>();
        for (Long partId : PartLockOrder.of(requiredByPart.keySet(), withdrawnByPart.keySet(), reservedPartIds)) {
            int difference = requiredByPart.getOrDefault(partId, 0) - withdrawnByPart.getOrDefault(partId, 0);
            if (difference != 0 || reservedPartIds.contains(partId)) {
                differenceByPart.put(partId, difference);
            }
        }

        List<Part> parts = PartLockOrder.lock(differenceByPart.keySet(), partRepository);
        releaseServiceOrderReservationUseCase.execute(
                new ReleaseServiceOrderReservationCommand(command.serviceOrderId()));
        for (Part part : parts) {
            move(part, differenceByPart.getOrDefault(part.getId(), 0), command);
        }
    }

    private void move(Part part, int difference, ReconcileServiceOrderStockCommand command) {
        if (difference > 0) {
            stockMovementRepository.save(StockMovement.withdrawal(
                    part, difference, command.serviceOrderId(), command.userId()));
        } else if (difference < 0) {
            stockMovementRepository.save(StockMovement.serviceOrderReturn(
                    part, -difference, command.serviceOrderId(), command.userId()));
        }
        partRepository.save(part);
    }
}
