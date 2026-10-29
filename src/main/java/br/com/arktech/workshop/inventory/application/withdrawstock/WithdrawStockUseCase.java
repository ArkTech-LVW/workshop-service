package br.com.arktech.workshop.inventory.application.withdrawstock;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.Map;
import java.util.TreeMap;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Joins the caller's transaction (quote approval), so a part without enough stock rolls back the whole approval.
 * Parts are locked in id order so that concurrent withdrawals cannot deadlock.
 */
@Service
@RequiredArgsConstructor
public class WithdrawStockUseCase implements VoidUseCase<WithdrawStockCommand> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public void execute(WithdrawStockCommand command) {
        Map<Long, Integer> quantityByPart = new TreeMap<>();
        command.items().forEach(item -> quantityByPart.merge(item.partId(), item.quantity(), Math::addExact));

        quantityByPart.forEach((partId, quantity) -> {
            Part part = partRepository.findRequiredForUpdateById(partId);
            StockMovement movement =
                    StockMovement.withdrawal(part, quantity, command.serviceOrderId(), command.userId());
            partRepository.save(part);
            stockMovementRepository.save(movement);
        });
    }
}
