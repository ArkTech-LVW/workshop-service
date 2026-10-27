package br.com.arktech.workshop.inventory.application.liststockmovements;

import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStockMovementsUseCase implements UseCase<ListStockMovementsQuery, Page<StockMovementResult>> {

    private final PartRepository partRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResult> execute(ListStockMovementsQuery query) {
        Part part = partRepository.findRequiredById(query.partId());
        Pagination pagination = new Pagination(query.page(), query.size());
        return stockMovementRepository.findPageByPartId(part.getId(), pagination).map(StockMovementResult::from);
    }
}
