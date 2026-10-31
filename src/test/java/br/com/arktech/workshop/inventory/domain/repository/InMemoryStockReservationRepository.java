package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.StockReservation;

import java.util.ArrayList;
import java.util.List;

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
    public List<StockReservation> findByServiceOrderId(Long serviceOrderId) {
        return reservations.stream()
                .filter(reservation -> reservation.getServiceOrderId().equals(serviceOrderId))
                .toList();
    }

    @Override
    public List<Long> findPartIdsByServiceOrderId(Long serviceOrderId) {
        return reservations.stream()
                .filter(reservation -> reservation.getServiceOrderId().equals(serviceOrderId))
                .map(reservation -> reservation.getPart().getId())
                .toList();
    }

    public List<StockReservation> findAll() {
        return List.copyOf(reservations);
    }
}
