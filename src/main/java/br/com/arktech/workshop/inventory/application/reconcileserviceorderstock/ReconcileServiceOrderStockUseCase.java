package br.com.arktech.workshop.inventory.application.reconcileserviceorderstock;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

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

    @Override
    @Transactional
    public void execute(ReconcileServiceOrderStockCommand command) {
        Map<Long, Integer> requiredByPart = requiredQuantityByPart(command.items());
        Map<Long, Integer> withdrawnByPart =
                stockMovementRepository.findNetWithdrawnByServiceOrderId(command.serviceOrderId());

        for (Long partId : partIdsInLockOrder(requiredByPart, withdrawnByPart)) {
            int required = requiredByPart.getOrDefault(partId, 0);
            int withdrawn = withdrawnByPart.getOrDefault(partId, 0);
            if (required > withdrawn) {
                withdraw(partId, required - withdrawn, command);
            } else if (required < withdrawn) {
                giveBack(partId, withdrawn - required, command);
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
            Map<Long, Integer> requiredByPart, Map<Long, Integer> withdrawnByPart) {
        SortedSet<Long> partIds = new TreeSet<>(requiredByPart.keySet());
        partIds.addAll(withdrawnByPart.keySet());
        return partIds;
    }

    private void withdraw(Long partId, int quantity, ReconcileServiceOrderStockCommand command) {
        Part part = partRepository.findRequiredForUpdateById(partId);
        save(part, StockMovement.withdrawal(part, quantity, command.serviceOrderId(), command.userId()));
    }

    private void giveBack(Long partId, int quantity, ReconcileServiceOrderStockCommand command) {
        Part part = partRepository.findRequiredForUpdateById(partId);
        save(part, StockMovement.serviceOrderReturn(part, quantity, command.serviceOrderId(), command.userId()));
    }

    private void save(Part part, StockMovement movement) {
        partRepository.save(part);
        stockMovementRepository.save(movement);
    }
}
