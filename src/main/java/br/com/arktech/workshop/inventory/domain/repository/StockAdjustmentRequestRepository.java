package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

public interface StockAdjustmentRequestRepository {

    StockAdjustmentRequest save(StockAdjustmentRequest request);

    Optional<StockAdjustmentRequest> findForUpdateById(Long id);

    Page<StockAdjustmentRequest> findPage(@Nullable StockAdjustmentRequestStatus status, Pagination pagination);

    default StockAdjustmentRequest findRequiredForUpdateById(Long id) {
        return findForUpdateById(id)
                .orElseThrow(() -> new NotFoundException("stockAdjustmentRequest.notFound", id.toString()));
    }
}
