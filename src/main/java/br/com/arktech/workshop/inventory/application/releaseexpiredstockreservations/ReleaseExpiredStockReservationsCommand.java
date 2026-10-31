package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import java.time.Instant;

public record ReleaseExpiredStockReservationsCommand(Instant now, int limit) {
}
