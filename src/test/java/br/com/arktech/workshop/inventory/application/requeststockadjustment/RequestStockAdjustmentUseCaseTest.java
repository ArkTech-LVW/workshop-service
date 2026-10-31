package br.com.arktech.workshop.inventory.application.requeststockadjustment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RequestStockAdjustmentUseCaseTest {

    private static final Long USER_ID = 7L;

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockAdjustmentRequestRepository stockAdjustmentRequestRepository =
            new InMemoryStockAdjustmentRequestRepository();
    private final RequestStockAdjustmentUseCase requestStockAdjustmentUseCase =
            new RequestStockAdjustmentUseCase(partRepository, stockAdjustmentRequestRepository, () -> USER_ID);

    @Test
    void shouldOpenPendingRequestByCurrentUserWithoutChangingStock() {
        Part part = savePart();
        part.increaseStock(5);

        StockAdjustmentRequestResult result = requestStockAdjustmentUseCase.execute(
                new RequestStockAdjustmentCommand(part.getId(), 2, "Avaria"));

        assertThat(result.id()).isNotNull();
        assertThat(result.partId()).isEqualTo(part.getId());
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.reason()).isEqualTo("Avaria");
        assertThat(result.status()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
        assertThat(result.requestedByUserId()).isEqualTo(USER_ID);
        assertThat(part.getStockQuantity()).isEqualTo(5);
        assertThat(stockAdjustmentRequestRepository.findPage(null, new Pagination(0, 10)).totalElements())
                .isEqualTo(1);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> requestStockAdjustmentUseCase.execute(
                        new RequestStockAdjustmentCommand(1234L, 1, "Avaria")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private Part savePart() {
        return partRepository.save(new Part(
                new Sku("FILTRO-01"), "Filtro", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
    }
}
