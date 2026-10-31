package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Map;

public interface StockMovementRepository {

    StockMovement save(StockMovement stockMovement);

    Page<StockMovement> findPageByPartId(Long partId, Pagination pagination);

    Map<Long, Integer> findNetWithdrawnByServiceOrderId(Long serviceOrderId);
}
