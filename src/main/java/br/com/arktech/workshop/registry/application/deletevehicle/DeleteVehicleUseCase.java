package br.com.arktech.workshop.registry.application.deletevehicle;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteVehicleUseCase implements VoidUseCase<DeleteVehicleCommand> {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public void execute(DeleteVehicleCommand command) {
        Vehicle vehicle = vehicleRepository.findRequiredById(command.id());

        vehicleRepository.remove(vehicle);
    }
}
