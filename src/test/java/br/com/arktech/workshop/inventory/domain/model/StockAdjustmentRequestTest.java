package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class StockAdjustmentRequestTest {

    private static final Long REQUESTER_ID = 7L;
    private static final Long REVIEWER_ID = 8L;
    private static final Instant REVIEWED_AT = Instant.parse("2026-10-30T12:00:00Z");

    private Part part;

    @BeforeEach
    void setUp() {
        part = new Part(
                new Sku("FILTRO-01"), "Filtro de óleo", PartType.PART, MeasurementUnit.UNIT, new Money(BigDecimal.TEN), 1);
        ReflectionTestUtils.setField(part, "id", 3L);
        part.increaseStock(5);
    }

    @Test
    void shouldOpenPendingRequestWithoutChangingStock() {
        StockAdjustmentRequest request = newRequest(2);

        assertThat(part.getStockQuantity()).isEqualTo(5);
        assertThat(request.getPart()).isSameAs(part);
        assertThat(request.getQuantity()).isEqualTo(2);
        assertThat(request.getReason()).isEqualTo("Avaria no estoque");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
        assertThat(request.getRequestedByUserId()).isEqualTo(REQUESTER_ID);
        assertThat(request.getReviewedByUserId()).isNull();
        assertThat(request.getReviewNote()).isNull();
        assertThat(request.getReviewedAt()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRequireReason(String reason) {
        assertThatThrownBy(() -> new StockAdjustmentRequest(part, 1, reason, REQUESTER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.reasonRequired");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRequirePositiveQuantity(int quantity) {
        assertThatThrownBy(() -> new StockAdjustmentRequest(part, quantity, "Avaria", REQUESTER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockMovement.quantityNotPositive");
    }

    @Test
    void shouldApproveAndRecordAdjustmentLinkedToRequest() {
        StockAdjustmentRequest request = newRequest(2);

        StockMovement movement = request.approve(REVIEWER_ID, REVIEWED_AT);

        assertThat(part.getStockQuantity()).isEqualTo(3);
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.APPROVED);
        assertThat(request.getReviewedByUserId()).isEqualTo(REVIEWER_ID);
        assertThat(request.getReviewedAt()).isEqualTo(REVIEWED_AT);
        assertThat(request.getReviewNote()).isNull();
        assertThat(movement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(movement.getQuantity()).isEqualTo(2);
        assertThat(movement.getReason()).isEqualTo("Avaria no estoque");
        assertThat(movement.getCreatedByUserId()).isEqualTo(REVIEWER_ID);
        assertThat(movement.getStockAdjustmentRequest()).isSameAs(request);
    }

    @Test
    void shouldKeepRequestPendingWhenStockIsInsufficient() {
        StockAdjustmentRequest request = newRequest(6);

        assertThatThrownBy(() -> request.approve(REVIEWER_ID, REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectWithReviewNote() {
        StockAdjustmentRequest request = newRequest(2);

        request.reject(REVIEWER_ID, "  Peça encontrada no almoxarifado ", REVIEWED_AT);

        assertThat(part.getStockQuantity()).isEqualTo(5);
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.REJECTED);
        assertThat(request.getReviewedByUserId()).isEqualTo(REVIEWER_ID);
        assertThat(request.getReviewNote()).isEqualTo("Peça encontrada no almoxarifado");
        assertThat(request.getReviewedAt()).isEqualTo(REVIEWED_AT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRequireReviewNoteOnRejection(String reviewNote) {
        StockAdjustmentRequest request = newRequest(2);

        assertThatThrownBy(() -> request.reject(REVIEWER_ID, reviewNote, REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.reviewNoteRequired");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
    }

    @Test
    void shouldNotReviewApprovedRequestAgain() {
        StockAdjustmentRequest request = newRequest(1);
        request.approve(REVIEWER_ID, REVIEWED_AT);

        assertThatThrownBy(() -> request.approve(REVIEWER_ID, REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.alreadyReviewed");
        assertThatThrownBy(() -> request.reject(REVIEWER_ID, "Engano", REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.alreadyReviewed");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.APPROVED);
        assertThat(part.getStockQuantity()).isEqualTo(4);
    }

    @Test
    void shouldNotReviewRejectedRequestAgain() {
        StockAdjustmentRequest request = newRequest(1);
        request.reject(REVIEWER_ID, "Engano", REVIEWED_AT);

        assertThatThrownBy(() -> request.approve(REVIEWER_ID, REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.alreadyReviewed");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.REJECTED);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldNotLetRequesterReviewOwnRequest() {
        StockAdjustmentRequest request = newRequest(1);

        assertThatThrownBy(() -> request.approve(REQUESTER_ID, REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.selfReview");
        assertThatThrownBy(() -> request.reject(REQUESTER_ID, "Engano", REVIEWED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("stockAdjustmentRequest.selfReview");
        assertThat(request.getStatus()).isEqualTo(StockAdjustmentRequestStatus.PENDING);
        assertThat(part.getStockQuantity()).isEqualTo(5);
    }

    private StockAdjustmentRequest newRequest(int quantity) {
        StockAdjustmentRequest request = new StockAdjustmentRequest(part, quantity, " Avaria no estoque ", REQUESTER_ID);
        ReflectionTestUtils.setField(request, "id", 11L);
        return request;
    }
}
