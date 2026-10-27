package br.com.arktech.workshop.inventory.application.listparts;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartFilter;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPartsUseCase implements UseCase<ListPartsQuery, Page<PartResult>> {

    private final PartRepository partRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PartResult> execute(ListPartsQuery query) {
        Sku sku = query.sku() == null ? null : new Sku(query.sku());
        PartFilter filter = new PartFilter(query.name(), sku, query.type());
        Pagination pagination = new Pagination(query.page(), query.size());
        return partRepository.findPage(filter, pagination).map(PartResult::from);
    }
}
