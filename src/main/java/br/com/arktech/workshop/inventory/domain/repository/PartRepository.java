package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.List;
import java.util.Optional;

public interface PartRepository {

    Part save(Part part);

    Optional<Part> findById(Long id);

    List<Part> findAllById(Iterable<Long> ids);

    boolean existsBySku(Sku sku);

    boolean existsBySkuAndIdNot(Sku sku, Long id);

    Optional<Part> findForUpdateById(Long id);

    Page<Part> findPage(PartFilter filter, Pagination pagination);

    Page<Part> findLowStockPage(Pagination pagination);

    void remove(Part part);

    default Part findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("part.notFound", id.toString()));
    }

    default Part findRequiredForUpdateById(Long id) {
        return findForUpdateById(id).orElseThrow(() -> new NotFoundException("part.notFound", id.toString()));
    }
}
