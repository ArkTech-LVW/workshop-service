package br.com.arktech.workshop.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequest;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.inventory.domain.model.StockMovement;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockAdjustmentRequestRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockMovementRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class StockAdjustmentRequestRepositoryTest {

    private static final Pagination FIRST_PAGE = new Pagination(0, 100);

    @Autowired
    private StockAdjustmentRequestRepository stockAdjustmentRequestRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Part part;
    private Long requesterId;
    private Long reviewerId;

    @BeforeEach
    void setUp() {
        part = new Part(
                new Sku("zzz-" + UUID.randomUUID().toString().substring(0, 8)),
                "Filtro",
                PartType.PART,
                MeasurementUnit.UNIT,
                new Money(BigDecimal.TEN),
                0);
        part.increaseStock(5);
        part = partRepository.save(part);
        requesterId = insertUser();
        reviewerId = insertUser();
    }

    @Test
    void shouldPersistApprovedRequestAndLinkTheMovement() {
        StockAdjustmentRequest request = stockAdjustmentRequestRepository.save(
                new StockAdjustmentRequest(part, 2, "Avaria", requesterId));
        Instant reviewedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        StockMovement movement = stockMovementRepository.save(request.approve(reviewerId, reviewedAt));
        flushAndClear();

        StockAdjustmentRequest found = stockAdjustmentRequestRepository.findRequiredForUpdateById(request.getId());

        assertThat(found.getPart().getId()).isEqualTo(part.getId());
        assertThat(found.getQuantity()).isEqualTo(2);
        assertThat(found.getReason()).isEqualTo("Avaria");
        assertThat(found.getStatus()).isEqualTo(StockAdjustmentRequestStatus.APPROVED);
        assertThat(found.getRequestedByUserId()).isEqualTo(requesterId);
        assertThat(found.getReviewedByUserId()).isEqualTo(reviewerId);
        assertThat(found.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT stock_adjustment_request_id FROM stock_movement WHERE id = ?",
                        Long.class,
                        movement.getId()))
                .isEqualTo(request.getId());
    }

    @Test
    void shouldPageByStatusOldestFirst() {
        StockAdjustmentRequest first = stockAdjustmentRequestRepository.save(
                new StockAdjustmentRequest(part, 1, "Avaria", requesterId));
        StockAdjustmentRequest rejected = stockAdjustmentRequestRepository.save(
                new StockAdjustmentRequest(part, 1, "Perda", requesterId));
        rejected.reject(reviewerId, "Engano", Instant.now());
        StockAdjustmentRequest second = stockAdjustmentRequestRepository.save(
                new StockAdjustmentRequest(part, 1, "Avaria", requesterId));
        flushAndClear();

        Page<StockAdjustmentRequest> pending =
                stockAdjustmentRequestRepository.findPage(StockAdjustmentRequestStatus.PENDING, FIRST_PAGE);
        Page<StockAdjustmentRequest> all = stockAdjustmentRequestRepository.findPage(null, FIRST_PAGE);

        assertThat(pending.content())
                .extracting(StockAdjustmentRequest::getId)
                .containsSubsequence(first.getId(), second.getId())
                .doesNotContain(rejected.getId());
        assertThat(all.content())
                .extracting(StockAdjustmentRequest::getId)
                .containsSubsequence(first.getId(), rejected.getId(), second.getId());
    }

    @Test
    void shouldRejectSelfReviewInDatabase() {
        StockAdjustmentRequest request = stockAdjustmentRequestRepository.save(
                new StockAdjustmentRequest(part, 1, "Avaria", requesterId));
        flushAndClear();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        UPDATE stock_adjustment_request
                        SET status = 'APPROVED', reviewed_by_user_id = requested_by_user_id, reviewed_at = now()
                        WHERE id = ?
                        """,
                        request.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReportUnknownRequest() {
        assertThatThrownBy(() -> stockAdjustmentRequestRepository.findRequiredForUpdateById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("stockAdjustmentRequest.notFound");
    }

    private Long insertUser() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO "user" (name, email, password_hash, date_created, last_updated, version)
                VALUES ('Estoquista', ?, 'hash', now(), now(), 0) RETURNING id
                """,
                Long.class,
                "stock-" + UUID.randomUUID() + "@arktech.com.br");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
