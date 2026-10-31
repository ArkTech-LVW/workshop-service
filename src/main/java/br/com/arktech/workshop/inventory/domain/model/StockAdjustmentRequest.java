package br.com.arktech.workshop.inventory.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "stock_adjustment_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAdjustmentRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    private int quantity;

    private String reason;

    @Enumerated(EnumType.STRING)
    private StockAdjustmentRequestStatus status;

    private Long requestedByUserId;

    private @Nullable Long reviewedByUserId;

    private @Nullable String reviewNote;

    private @Nullable Instant reviewedAt;

    public StockAdjustmentRequest(Part part, int quantity, @Nullable String reason, Long requestedByUserId) {
        if (quantity <= 0) {
            throw new BusinessRuleException("stockMovement.quantityNotPositive");
        }

        this.part = part;
        this.quantity = quantity;
        this.reason = requireText(reason, "stockMovement.reasonRequired");
        this.status = StockAdjustmentRequestStatus.PENDING;
        this.requestedByUserId = requestedByUserId;
    }

    public StockMovement approve(Long reviewerId, Instant reviewedAt) {
        requireReviewableBy(reviewerId);
        StockMovement movement = StockMovement.adjustment(this, reviewerId);
        markReviewed(StockAdjustmentRequestStatus.APPROVED, reviewerId, null, reviewedAt);
        return movement;
    }

    public void reject(Long reviewerId, @Nullable String reviewNote, Instant reviewedAt) {
        requireReviewableBy(reviewerId);
        String note = requireText(reviewNote, "stockAdjustmentRequest.reviewNoteRequired");
        markReviewed(StockAdjustmentRequestStatus.REJECTED, reviewerId, note, reviewedAt);
    }

    private void requireReviewableBy(Long reviewerId) {
        if (status != StockAdjustmentRequestStatus.PENDING) {
            throw new BusinessRuleException("stockAdjustmentRequest.alreadyReviewed", getId());
        }
        if (requestedByUserId.equals(reviewerId)) {
            throw new BusinessRuleException("stockAdjustmentRequest.selfReview");
        }
    }

    private void markReviewed(
            StockAdjustmentRequestStatus status, Long reviewerId, @Nullable String reviewNote, Instant reviewedAt) {
        this.status = status;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = reviewNote;
        this.reviewedAt = reviewedAt;
    }

    private static String requireText(@Nullable String text, String code) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException(code);
        }
        return text.strip();
    }
}
