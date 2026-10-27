package br.com.arktech.workshop.inventory.application.listlowstockparts;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListLowStockPartsUseCase implements UseCase<ListLowStockPartsQuery, Page<PartResult>> {

    private final PartRepository partRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PartResult> execute(ListLowStockPartsQuery query) {
        Pagination pagination = new Pagination(query.page(), query.size());
        return partRepository.findLowStockPage(pagination).map(PartResult::from);
    }
}
