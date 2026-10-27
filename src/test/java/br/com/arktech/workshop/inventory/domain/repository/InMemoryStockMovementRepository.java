package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryStockMovementRepository implements StockMovementRepository {

    private final List<StockMovement> movements = new ArrayList<>();

    private long nextId = 1;

    @Override
    public StockMovement save(StockMovement stockMovement) {
        if (!movements.contains(stockMovement)) {
            ReflectionTestUtils.setField(stockMovement, "id", nextId++);
            movements.add(stockMovement);
        }
        return stockMovement;
    }

    @Override
    public Page<StockMovement> findPageByPartId(Long partId, Pagination pagination) {
        List<StockMovement> matching = movements.stream()
                .filter(movement -> movement.getPartId().equals(partId))
                .sorted(Comparator.comparing(StockMovement::getId).reversed())
                .toList();
        List<StockMovement> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    public List<StockMovement> findAll() {
        return List.copyOf(movements);
    }
}
