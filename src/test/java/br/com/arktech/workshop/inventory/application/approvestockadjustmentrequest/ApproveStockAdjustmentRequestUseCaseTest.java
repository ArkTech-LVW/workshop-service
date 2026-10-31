package br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryPartRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockAdjustmentRequestRepository;
import br.com.arktech.workshop.inventory.domain.repository.InMemoryStockMovementRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ApproveStockAdjustmentRequestUseCaseTest {

    private static final Long REQUESTER_ID = 7L;
    private static final Long REVIEWER_ID = 8L;
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    private final InMemoryPartRepository partRepository = new InMemoryPartRepository();
    private final InMemoryStockMovementRepository stockMovementRepository = new InMemoryStockMovementRepository();
    private final InMemoryStockAdjustmentRequestRepository stockAdjustmentRequestRepository =
            new InMemoryStockAdjustmentRequestRepository();
    private final ApproveStockAdjustmentRequestUseCase approveStockAdjustmentRequestUseCase =
            new ApproveStockAdjustmentRequestUseCase(
                    stockAdjustmentRequestRepository,
                    partRepository,
                    stockMovementRepository,
                    () -> REVIEWER_ID,
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldApproveDecreaseStockAndRecordLinkedAdjustment() {
        Part part = savePart(5);
        StockAdjustmentRequest request = saveRequest(part, 2, REQUESTER_ID);

        StockAdjustmentRequestResult result =
                approveStockAdjustmentRequestUseCase.execute(new ApproveStockAdjustmentRequestCommand(request.getId()));

        assertThat(result.status()).isEqualTo(StockAdjustmentRequestStatus.APPROVED);
        assertThat(result.reviewedByUserId()).isEqualTo(REVIEWER_ID);
        assertThat(result.reviewedAt()).isEqualTo(NOW);
        assertThat(part.getStockQuantity()).isEqualTo(3);
        assertThat(stockMovementRepository.findAll()).singleElement().satisfies(movement -> {
            assertThat(movement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
            assertThat(movement.getQuantity()).isEqualTo(2);
            assertThat(movement.getReason()).isEqualTo("Avaria");
            assertThat(movement.getCreatedByUserId()).isEqualTo(REVIEWER_ID);
            assertThat(movement.getStockAdjustmentRequest()).isSameAs(request);
        });
    }

    @Test
    void shouldRefuseApprovalWithoutEnoughStock() {
        Part part = savePart(1);
        StockAdjustmentRequest request = saveRequest(part, 2, REQUESTER_ID);

        assertThatThrownBy(() -> approveStockAdjustmentRequestUseCase.execute(
                        new ApproveStockAdjustmentRequestCommand(request.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
        assertThat(part.getStockQuantity()).isEqualTo(1);
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldNotLetRequesterApproveOwnRequest() {
        Part part = savePart(5);
        StockAdjustmentRequest request = saveRequest(part, 2, REVIEWER_ID);

        assertThatThrownBy(() -> approveStockAdjustmentRequestUseCase.execute(
                        new ApproveStockAdjustmentRequestCommand(request.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.selfReview");
        assertThat(part.getStockQuantity()).isEqualTo(5);
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldNotApproveTwice() {
        Part part = savePart(5);
        StockAdjustmentRequest request = saveRequest(part, 2, REQUESTER_ID);
        ApproveStockAdjustmentRequestCommand command = new ApproveStockAdjustmentRequestCommand(request.getId());
        approveStockAdjustmentRequestUseCase.execute(command);

        assertThatThrownBy(() -> approveStockAdjustmentRequestUseCase.execute(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.alreadyReviewed");
        assertThat(part.getStockQuantity()).isEqualTo(3);
        assertThat(stockMovementRepository.findAll()).extracting(StockMovement::getId).hasSize(1);
    }

    @Test
    void shouldReportUnknownRequest() {
        assertThatThrownBy(() -> approveStockAdjustmentRequestUseCase.execute(
                        new ApproveStockAdjustmentRequestCommand(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("stockAdjustmentRequest.notFound");
    }

    private Part savePart(int stock) {
        Part part = partRepository.save(new Part(
                new Sku("FILTRO-01"), "Filtro", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1));
        part.increaseStock(stock);
        return part;
    }

    private StockAdjustmentRequest saveRequest(Part part, int quantity, Long requesterId) {
        return stockAdjustmentRequestRepository.save(new StockAdjustmentRequest(part, quantity, "Avaria", requesterId));
    }
}
