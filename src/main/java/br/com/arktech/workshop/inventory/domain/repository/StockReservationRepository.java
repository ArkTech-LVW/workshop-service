package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockReservation;

import java.time.Instant;
import java.util.List;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    void delete(StockReservation stockReservation);

    List<StockReservation> findByServiceOrderId(Long serviceOrderId);

    List<Long> findPartIdsByServiceOrderId(Long serviceOrderId);

    List<Long> findPartIdsWithReservationsExpiredBefore(Instant instant, int limit);

    List<StockReservation> findByPartIdAndExpiresAtBefore(Long partId, Instant instant);
}
