package br.com.arktech.workshop.inventory.application.requeststockadjustment;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestStockAdjustmentUseCase
        implements UseCase<RequestStockAdjustmentCommand, StockAdjustmentRequestResult> {

    private final PartRepository partRepository;
    private final StockAdjustmentRequestRepository stockAdjustmentRequestRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public StockAdjustmentRequestResult execute(RequestStockAdjustmentCommand command) {
        Part part = partRepository.findRequiredById(command.partId());
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(part, command.quantity(), command.reason(), currentUser.id());
        return StockAdjustmentRequestResult.from(stockAdjustmentRequestRepository.save(request));
    }
}
