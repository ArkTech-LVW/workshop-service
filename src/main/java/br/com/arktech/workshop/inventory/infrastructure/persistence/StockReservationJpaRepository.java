package br.com.arktech.workshop.inventory.infrastructure.persistence;

import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockReservationJpaRepository extends JpaRepository<StockReservation, Long>, StockReservationRepository {

    @Override
    @Query("SELECT r.part.id FROM StockReservation r WHERE r.serviceOrderId = :serviceOrderId")
    List<Long> findPartIdsByServiceOrderId(@Param("serviceOrderId") Long serviceOrderId);

    @Override
    default List<Long> findPartIdsWithReservationsExpiredBefore(Instant instant, int limit) {
        return findPartIdsExpiredBefore(instant, Limit.of(limit));
    }

    @Query("SELECT DISTINCT r.part.id FROM StockReservation r WHERE r.expiresAt < :instant ORDER BY r.part.id")
    List<Long> findPartIdsExpiredBefore(@Param("instant") Instant instant, Limit limit);
}
