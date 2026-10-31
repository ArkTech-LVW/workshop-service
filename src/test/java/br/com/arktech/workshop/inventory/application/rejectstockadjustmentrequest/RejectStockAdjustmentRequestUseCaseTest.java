package br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockAdjustmentRequestRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RejectStockAdjustmentRequestUseCaseTest {

    private static final Long REQUESTER_ID = 7L;
    private static final Long REVIEWER_ID = 8L;
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    private final InMemoryStockAdjustmentRequestRepository stockAdjustmentRequestRepository =
            new InMemoryStockAdjustmentRequestRepository();
    private final RejectStockAdjustmentRequestUseCase rejectStockAdjustmentRequestUseCase =
            new RejectStockAdjustmentRequestUseCase(
                    stockAdjustmentRequestRepository, () -> REVIEWER_ID, Clock.fixed(NOW, ZoneOffset.UTC));

    private Part part;

    @BeforeEach
    void setUp() {
        part = new Part(
                new Sku("FILTRO-01"), "Filtro", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1);
        ReflectionTestUtils.setField(part, "id", 3L);
        part.increaseStock(5);
    }

    @Test
    void shouldRejectWithReviewNoteWithoutChangingStock() {
        StockAdjustmentRequest request = saveRequest(REQUESTER_ID);

        StockAdjustmentRequestResult result = rejectStockAdjustmentRequestUseCase.execute(
                new RejectStockAdjustmentRequestCommand(request.getId(), "Peça encontrada"));

        assertThat(result.status()).isEqualTo(StockAdjustmentRequestStatus.REJECTED);
        assertThat(result.reviewedByUserId()).isEqualTo(REVIEWER_ID);
        assertThat(result.reviewNote()).isEqualTo("Peça encontrada");
        assertThat(result.reviewedAt()).isEqualTo(NOW);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRequireReviewNote() {
        StockAdjustmentRequest request = saveRequest(REQUESTER_ID);

        assertThatThrownBy(() -> rejectStockAdjustmentRequestUseCase.execute(
                        new RejectStockAdjustmentRequestCommand(request.getId(), " ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.reviewNoteRequired");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
    }

    @Test
    void shouldNotLetRequesterRejectOwnRequest() {
        StockAdjustmentRequest request = saveRequest(REVIEWER_ID);

        assertThatThrownBy(() -> rejectStockAdjustmentRequestUseCase.execute(
                        new RejectStockAdjustmentRequestCommand(request.getId(), "Engano")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.selfReview");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
    }

    @Test
    void shouldNotRejectTwice() {
        StockAdjustmentRequest request = saveRequest(REQUESTER_ID);
        RejectStockAdjustmentRequestCommand command = new RejectStockAdjustmentRequestCommand(request.getId(), "Engano");
        rejectStockAdjustmentRequestUseCase.execute(command);

        assertThatThrownBy(() -> rejectStockAdjustmentRequestUseCase.execute(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.alreadyReviewed");
    }

    private StockAdjustmentRequest saveRequest(Long requesterId) {
        return stockAdjustmentRequestRepository.save(new StockAdjustmentRequest(part, 1, "Avaria", requesterId));
    }
}
