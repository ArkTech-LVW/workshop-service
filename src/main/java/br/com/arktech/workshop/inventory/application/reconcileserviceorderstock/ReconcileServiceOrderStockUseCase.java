package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconcileServiceOrderStockUseCase implements VoidUseCase<ReconcileServiceOrderStockCommand> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional
    public void execute(ReconcileServiceOrderStockCommand command) {
        Map<Long, Integer> requiredByPart = requiredQuantityByPart(command.items());
        Map<Long, Integer> withdrawnByPart =
                stockMovementRepository.findNetWithdrawnByServiceOrderId(command.serviceOrderId());
        List<Long> reservedPartIds = stockReservationRepository.findPartIdsByServiceOrderId(command.serviceOrderId());

        for (Long partId : partIdsInLockOrder(requiredByPart.keySet(), withdrawnByPart.keySet(), reservedPartIds)) {
            int required = requiredByPart.getOrDefault(partId, 0);
            int withdrawn = withdrawnByPart.getOrDefault(partId, 0);
            if (required != withdrawn || reservedPartIds.contains(partId)) {
                reconcile(partId, required, withdrawn, command);
            }
        }
    }

    private Map<Long, Integer> requiredQuantityByPart(List<ReconcileServiceOrderStockCommand.Item> items) {
        return items.stream().collect(Collectors.toMap(
                ReconcileServiceOrderStockCommand.Item::partId,
                ReconcileServiceOrderStockCommand.Item::quantity,
                Math::addExact));
    }

    private SortedSet<Long> partIdsInLockOrder(
            Collection<Long> requiredPartIds, Collection<Long> withdrawnPartIds, Collection<Long> reservedPartIds) {
        SortedSet<Long> partIds = new TreeSet<>(requiredPartIds);
        partIds.addAll(withdrawnPartIds);
        partIds.addAll(reservedPartIds);
        return partIds;
    }

    private void reconcile(Long partId, int required, int withdrawn, ReconcileServiceOrderStockCommand command) {
        Part part = partRepository.findRequiredForUpdateById(partId);
        releaseReservation(partId, command.serviceOrderId());
        if (required > withdrawn) {
            stockMovementRepository.save(StockMovement.withdrawal(
                    part, required - withdrawn, command.serviceOrderId(), command.userId()));
        } else if (required < withdrawn) {
            stockMovementRepository.save(StockMovement.serviceOrderReturn(
                    part, withdrawn - required, command.serviceOrderId(), command.userId()));
        }
        partRepository.save(part);
    }

    private void releaseReservation(Long partId, Long serviceOrderId) {
        stockReservationRepository.findByServiceOrderIdAndPartId(serviceOrderId, partId).ifPresent(reservation -> {
            reservation.release();
            stockReservationRepository.delete(reservation);
        });
    }
}
