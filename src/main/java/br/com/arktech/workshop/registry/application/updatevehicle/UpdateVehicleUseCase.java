package br.com.arktech.workshop.registry.application.updatevehicle;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateVehicleUseCase implements UseCase<UpdateVehicleCommand, VehicleResult> {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResult execute(UpdateVehicleCommand command) {
        Vehicle vehicle = vehicleRepository.findRequiredById(command.id());

        LicensePlate licensePlate = new LicensePlate(command.licensePlate());
        if (vehicleRepository.existsByLicensePlateAndIdNot(licensePlate, vehicle.getId())) {
            throw new ConflictException("vehicle.licensePlateAlreadyExists", licensePlate.value());
        }

        Chassis chassis = command.chassis() == null ? null : new Chassis(command.chassis());
        if (chassis != null && vehicleRepository.existsByChassisAndIdNot(chassis, vehicle.getId())) {
            throw new ConflictException("vehicle.chassisAlreadyExists", chassis.value());
        }

        vehicle.changeIdentification(licensePlate, chassis);
        vehicle.repaint(command.color());
        return VehicleResult.from(vehicleRepository.save(vehicle));
    }
}
