package br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import java.time.Clock;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectStockAdjustmentRequestUseCase
        implements UseCase<RejectStockAdjustmentRequestCommand, StockAdjustmentRequestResult> {

    private final StockAdjustmentRequestRepository stockAdjustmentRequestRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Override
    @Transactional
    public StockAdjustmentRequestResult execute(RejectStockAdjustmentRequestCommand command) {
        StockAdjustmentRequest request = stockAdjustmentRequestRepository.findRequiredForUpdateById(command.requestId());
        request.reject(currentUser.id(), command.reviewNote(), clock.instant());
        return StockAdjustmentRequestResult.from(stockAdjustmentRequestRepository.save(request));
    }
}
