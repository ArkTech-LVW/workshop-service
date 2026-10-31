package br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import java.time.Clock;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveStockAdjustmentRequestUseCase
        implements UseCase<ApproveStockAdjustmentRequestCommand, StockAdjustmentRequestResult> {

    private final StockAdjustmentRequestRepository stockAdjustmentRequestRepository;
    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Override
    @Transactional
    public StockAdjustmentRequestResult execute(ApproveStockAdjustmentRequestCommand command) {
        StockAdjustmentRequest request = stockAdjustmentRequestRepository.findRequiredForUpdateById(command.requestId());
        Part part = partRepository.findRequiredForUpdateById(request.getPart().getId());
        StockMovement movement = request.approve(currentUser.id(), clock.instant());

        partRepository.save(part);
        stockMovementRepository.save(movement);

        return StockAdjustmentRequestResult.from(stockAdjustmentRequestRepository.save(request));
    }
}
