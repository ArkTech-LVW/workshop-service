package br.com.arktech.workshop.inventory.application.releaseexpiredstockreservations;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseExpiredStockReservationsUseCase implements VoidUseCase<ReleaseExpiredStockReservationsCommand> {

    private final PartRepository partRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional
    public void execute(ReleaseExpiredStockReservationsCommand command) {
        for (Long partId :
                stockReservationRepository.findPartIdsWithReservationsExpiredBefore(command.now(), command.limit())) {
            Part part = partRepository.findRequiredForUpdateById(partId);
            for (StockReservation reservation :
                    stockReservationRepository.findByPartIdAndExpiresAtBefore(partId, command.now())) {
                reservation.release();
                stockReservationRepository.delete(reservation);
            }
            partRepository.save(part);
        }
    }
}
