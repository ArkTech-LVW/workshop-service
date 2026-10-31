package br.com.arktech.workshop.inventory.application.liststockadjustmentrequests;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListStockAdjustmentRequestsUseCaseTest {

    private final InMemoryStockAdjustmentRequestRepository stockAdjustmentRequestRepository =
            new InMemoryStockAdjustmentRequestRepository();
    private final ListStockAdjustmentRequestsUseCase listStockAdjustmentRequestsUseCase =
            new ListStockAdjustmentRequestsUseCase(stockAdjustmentRequestRepository);

    @Test
    void shouldFilterByStatus() {
        Part part = new Part(
                new Sku("FILTRO-01"), "Filtro", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1);
        ReflectionTestUtils.setField(part, "id", 3L);
        StockAdjustmentRequest pending =
                stockAdjustmentRequestRepository.save(new StockAdjustmentRequest(part, 1, "Avaria", 7L));
        StockAdjustmentRequest rejected =
                stockAdjustmentRequestRepository.save(new StockAdjustmentRequest(part, 1, "Perda", 7L));
        rejected.reject(8L, "Engano", Instant.now());

        Page<StockAdjustmentRequestResult> onlyPending = listStockAdjustmentRequestsUseCase.execute(
                new ListStockAdjustmentRequestsQuery(StockAdjustmentRequestStatus.PENDING, 0, 20));
        Page<StockAdjustmentRequestResult> all =
                listStockAdjustmentRequestsUseCase.execute(new ListStockAdjustmentRequestsQuery(null, 0, 20));

        assertThat(onlyPending.content()).extracting(StockAdjustmentRequestResult::id).containsExactly(pending.getId());
        assertThat(all.content())
                .extracting(StockAdjustmentRequestResult::id)
                .containsExactly(pending.getId(), rejected.getId());
    }
}
