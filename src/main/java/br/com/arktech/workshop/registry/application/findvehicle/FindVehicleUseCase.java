package br.com.arktech.workshop.registry.application.findvehicle;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindVehicleUseCase implements UseCase<FindVehicleQuery, VehicleResult> {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public VehicleResult execute(FindVehicleQuery query) {
        return VehicleResult.from(vehicleRepository.findRequiredById(query.id()));
    }
}
