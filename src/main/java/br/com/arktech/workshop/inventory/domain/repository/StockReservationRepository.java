package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockReservation;

import java.util.List;
import java.util.Optional;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    void delete(StockReservation stockReservation);

    Optional<StockReservation> findByServiceOrderIdAndPartId(Long serviceOrderId, Long partId);

    List<Long> findPartIdsByServiceOrderId(Long serviceOrderId);
}
