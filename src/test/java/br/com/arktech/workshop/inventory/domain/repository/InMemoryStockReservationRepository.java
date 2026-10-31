package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockReservation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryStockReservationRepository implements StockReservationRepository {

    private final List<StockReservation> reservations = new ArrayList<>();

    private long nextId = 1;

    @Override
    public StockReservation save(StockReservation stockReservation) {
        if (!reservations.contains(stockReservation)) {
            ReflectionTestUtils.setField(stockReservation, "id", nextId++);
            reservations.add(stockReservation);
        }
        return stockReservation;
    }

    @Override
    public void delete(StockReservation stockReservation) {
        reservations.remove(stockReservation);
    }

    @Override
    public Optional<StockReservation> findByServiceOrderIdAndPartId(Long serviceOrderId, Long partId) {
        return reservations.stream()
                .filter(reservation -> reservation.getServiceOrderId().equals(serviceOrderId))
                .filter(reservation -> reservation.getPart().getId().equals(partId))
                .findFirst();
    }

    @Override
    public List<Long> findPartIdsByServiceOrderId(Long serviceOrderId) {
        return reservations.stream()
                .filter(reservation -> reservation.getServiceOrderId().equals(serviceOrderId))
                .map(reservation -> reservation.getPart().getId())
                .toList();
    }

    @Override
    public List<Long> findPartIdsWithReservationsExpiredBefore(Instant instant) {
        return reservations.stream()
                .filter(reservation -> reservation.getExpiresAt().isBefore(instant))
                .map(reservation -> reservation.getPart().getId())
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<StockReservation> findByPartIdAndExpiresAtBefore(Long partId, Instant instant) {
        return reservations.stream()
                .filter(reservation -> reservation.getPart().getId().equals(partId))
                .filter(reservation -> reservation.getExpiresAt().isBefore(instant))
                .toList();
    }

    public List<StockReservation> findAll() {
        return List.copyOf(reservations);
    }
}
