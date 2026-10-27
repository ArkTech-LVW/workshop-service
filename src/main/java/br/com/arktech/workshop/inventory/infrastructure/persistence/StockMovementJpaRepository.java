package br.com.arktech.workshop.inventory.infrastructure.persistence;

import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface StockMovementJpaRepository
        extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement>, StockMovementRepository {

    Sort PAGE_ORDER = Sort.by(Sort.Order.desc("dateCreated"), Sort.Order.desc("id"));

    @Override
    default Page<StockMovement> findPageByPartId(Long partId, Pagination pagination) {
        Specification<StockMovement> ofPart =
                (root, query, builder) -> builder.equal(root.get("part").get("id"), partId);
        var page = findAll(ofPart, PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }
}
