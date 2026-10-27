package br.com.arktech.workshop.inventory.application.registerstockmovement;

import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterStockMovementUseCase implements UseCase<RegisterStockMovementCommand, StockMovementResult> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public StockMovementResult execute(RegisterStockMovementCommand command) {
        Part part = partRepository.findRequiredForUpdateById(command.partId());
        Long createdBy = currentUser.id();

        StockMovement movement = switch (command.type()) {
            case IN -> StockMovement.entry(part, command.quantity(), command.reason(), createdBy);
            case ADJUSTMENT -> StockMovement.adjustment(part, command.quantity(), command.reason(), createdBy);
            case OUT -> throw new BusinessRuleException("stockMovement.outRequiresServiceOrder");
        };
        partRepository.save(part);
        return StockMovementResult.from(stockMovementRepository.save(movement));
    }
}
