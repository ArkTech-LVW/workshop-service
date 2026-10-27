package br.com.arktech.workshop.registry.application.createvehicle;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
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
public class CreateVehicleUseCase implements UseCase<CreateVehicleCommand, VehicleResult> {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public VehicleResult execute(CreateVehicleCommand command) {
        customerRepository.findRequiredById(command.customerId());

        LicensePlate licensePlate = new LicensePlate(command.licensePlate());
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new ConflictException("vehicle.licensePlateAlreadyExists", licensePlate.value());
        }

        Chassis chassis = command.chassis() == null ? null : new Chassis(command.chassis());
        if (chassis != null && vehicleRepository.existsByChassis(chassis)) {
            throw new ConflictException("vehicle.chassisAlreadyExists", chassis.value());
        }

        Vehicle vehicle = new Vehicle(
                command.customerId(),
                licensePlate,
                chassis,
                command.brand(),
                command.model(),
                command.year(),
                command.color());
        return VehicleResult.from(vehicleRepository.save(vehicle));
    }
}
