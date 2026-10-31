package br.com.arktech.workshop.inventory.application.releaseserviceorderreservation;

import br.com.arktech.workshop.inventory.application.serviceorderpart.PartLockOrder;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.StockReservation;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.inventory.domain.repository.StockReservationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseServiceOrderReservationUseCase implements VoidUseCase<ReleaseServiceOrderReservationCommand> {

    private final PartRepository partRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional
    public void execute(ReleaseServiceOrderReservationCommand command) {
        List<Part> parts = PartLockOrder.lock(
                stockReservationRepository.findPartIdsByServiceOrderId(command.serviceOrderId()), partRepository);
        for (StockReservation reservation : stockReservationRepository.findByServiceOrderId(command.serviceOrderId())) {
            reservation.release();
            stockReservationRepository.delete(reservation);
        }
        parts.forEach(partRepository::save);
    }
}
