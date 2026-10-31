package br.com.arktech.workshop.inventory.infrastructure.persistence;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

interface StockAdjustmentRequestJpaRepository
        extends JpaRepository<StockAdjustmentRequest, Long>,
                JpaSpecificationExecutor<StockAdjustmentRequest>,
                StockAdjustmentRequestRepository {

    Sort PAGE_ORDER = Sort.by(Sort.Order.asc("dateCreated"), Sort.Order.asc("id"));

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockAdjustmentRequest> findForUpdateById(Long id);

    @Override
    default Page<StockAdjustmentRequest> findPage(
            @Nullable StockAdjustmentRequestStatus status, Pagination pagination) {
        var page = findAll(statusEquals(status), PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    private static Specification<StockAdjustmentRequest> statusEquals(@Nullable StockAdjustmentRequestStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }
}
