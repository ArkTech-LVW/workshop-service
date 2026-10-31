package br.com.arktech.workshop.inventory.application.liststockadjustmentrequests;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStockAdjustmentRequestsUseCase
        implements UseCase<ListStockAdjustmentRequestsQuery, Page<StockAdjustmentRequestResult>> {

    private final StockAdjustmentRequestRepository stockAdjustmentRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StockAdjustmentRequestResult> execute(ListStockAdjustmentRequestsQuery query) {
        Pagination pagination = new Pagination(query.page(), query.size());
        return stockAdjustmentRequestRepository
                .findPage(query.status(), pagination)
                .map(StockAdjustmentRequestResult::from);
    }
}
