package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Optional;

public interface PartRepository {

    Part save(Part part);

    Optional<Part> findById(Long id);

    boolean existsBySku(Sku sku);

    Page<Part> findPage(PartFilter filter, Pagination pagination);

    default Part findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("part.notFound", id.toString()));
    }
}
