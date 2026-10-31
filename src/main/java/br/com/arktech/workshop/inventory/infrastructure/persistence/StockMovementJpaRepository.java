package br.com.arktech.workshop.inventory.infrastructure.persistence;

import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockMovementJpaRepository
        extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement>, StockMovementRepository {

    Sort PAGE_ORDER = Sort.by(Sort.Order.desc("dateCreated"), Sort.Order.desc("id"));

    @Override
    @EntityGraph(attributePaths = "stockAdjustmentRequest")
    org.springframework.data.domain.Page<StockMovement> findAll(
            @Nullable Specification<StockMovement> spec, Pageable pageable);

    @Override
    default Map<Long, Integer> findNetWithdrawnByServiceOrderId(Long serviceOrderId) {
        return sumNetWithdrawnByServiceOrderId(serviceOrderId).stream()
                .collect(Collectors.toMap(
                        NetWithdrawn::getPartId, netWithdrawn -> Math.toIntExact(netWithdrawn.getQuantity())));
    }

    @Query("""
            SELECT m.part.id AS partId,
                   SUM(CASE WHEN m.type = br.com.arktech.workshop.inventory.domain.model.StockMovementType.OUT
                            THEN m.quantity ELSE -m.quantity END) AS quantity
            FROM StockMovement m
            WHERE m.serviceOrderId = :serviceOrderId
              AND m.type IN (br.com.arktech.workshop.inventory.domain.model.StockMovementType.IN,
                             br.com.arktech.workshop.inventory.domain.model.StockMovementType.OUT)
            GROUP BY m.part.id""")
    List<NetWithdrawn> sumNetWithdrawnByServiceOrderId(@Param("serviceOrderId") Long serviceOrderId);

    interface NetWithdrawn {

        Long getPartId();

        Long getQuantity();
    }

    @Override
    default Page<StockMovement> findPageByPartId(Long partId, Pagination pagination) {
        Specification<StockMovement> ofPart =
                (root, query, builder) -> builder.equal(root.get("part").get("id"), partId);
        var page = findAll(ofPart, PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }
}
