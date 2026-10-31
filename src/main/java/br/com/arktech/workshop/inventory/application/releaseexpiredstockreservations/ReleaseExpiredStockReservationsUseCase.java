package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseExpiredStockReservationsUseCase
        implements UseCase<ReleaseExpiredStockReservationsCommand, ReleaseExpiredStockReservationsResult> {

    private final PartRepository partRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional
    public ReleaseExpiredStockReservationsResult execute(ReleaseExpiredStockReservationsCommand command) {
        List<Long> partIds = stockReservationRepository.findPartIdsWithReservationsExpiredBefore(
                command.now(), command.afterPartId(), command.limit());
        for (Long partId : partIds) {
            Part part = partRepository.findRequiredForUpdateById(partId);

            for (StockReservation reservation : stockReservationRepository.findByPartIdAndExpiresAtBefore(partId, command.now())) {
                reservation.release();
                stockReservationRepository.delete(reservation);
            }

            partRepository.save(part);
        }
        return new ReleaseExpiredStockReservationsResult(partIds.size(), partIds.isEmpty() ? command.afterPartId() : partIds.getLast());
    }
}
