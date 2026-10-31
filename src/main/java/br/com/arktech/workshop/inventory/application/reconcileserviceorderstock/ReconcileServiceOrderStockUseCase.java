package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Joins the caller's transaction (quote approval or cancellation), so a part without enough stock rolls back the
 * whole change. Parts are locked in id order so that concurrent reconciliations cannot deadlock.
 */
@Service
@RequiredArgsConstructor
public class ReconcileServiceOrderStockUseCase implements VoidUseCase<ReconcileServiceOrderStockCommand> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public void execute(ReconcileServiceOrderStockCommand command) {
        Map<Long, Integer> targetByPart = new TreeMap<>();
        command.items().forEach(item -> targetByPart.merge(item.partId(), item.quantity(), Math::addExact));
        Map<Long, Integer> withdrawnByPart =
                stockMovementRepository.findNetWithdrawnByServiceOrderId(command.serviceOrderId());

        TreeSet<Long> partIds = new TreeSet<>(targetByPart.keySet());
        partIds.addAll(withdrawnByPart.keySet());
        partIds.forEach(partId -> {
            int difference = targetByPart.getOrDefault(partId, 0) - withdrawnByPart.getOrDefault(partId, 0);
            if (difference != 0) {
                move(partId, difference, command);
            }
        });
    }

    private void move(Long partId, int difference, ReconcileServiceOrderStockCommand command) {
        Part part = partRepository.findRequiredForUpdateById(partId);
        StockMovement movement = difference > 0
                ? StockMovement.withdrawal(part, difference, command.serviceOrderId(), command.userId())
                : StockMovement.serviceOrderReturn(part, -difference, command.serviceOrderId(), command.userId());
        partRepository.save(part);
        stockMovementRepository.save(movement);
    }
}
