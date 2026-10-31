package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryStockAdjustmentRequestRepository implements StockAdjustmentRequestRepository {

    private final List<StockAdjustmentRequest> requests = new ArrayList<>();

    private long nextId = 1;

    @Override
    public StockAdjustmentRequest save(StockAdjustmentRequest request) {
        if (!requests.contains(request)) {
            ReflectionTestUtils.setField(request, "id", nextId++);
            requests.add(request);
        }
        return request;
    }

    @Override
    public Optional<StockAdjustmentRequest> findForUpdateById(Long id) {
        return requests.stream().filter(request -> request.getId().equals(id)).findFirst();
    }

    @Override
    public Page<StockAdjustmentRequest> findPage(@Nullable StockAdjustmentRequestStatus status, Pagination pagination) {
        List<StockAdjustmentRequest> matching = requests.stream()
                .filter(request -> status == null || request.getStatus() == status)
                .sorted(Comparator.comparing(StockAdjustmentRequest::getId))
                .toList();
        List<StockAdjustmentRequest> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }
}
