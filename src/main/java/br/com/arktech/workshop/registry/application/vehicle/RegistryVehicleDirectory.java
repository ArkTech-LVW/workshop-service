package br.com.arktech.workshop.registry.application.vehicle;

import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleCommand;
import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleUseCase;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RegistryVehicleDirectory implements VehicleDirectory {

    private final VehicleRepository vehicleRepository;
    private final CreateVehicleUseCase createVehicleUseCase;

    @Override
    public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
        return vehicleRepository
                .findByLicensePlate(new LicensePlate(vehicle.licensePlate()))
                .map(registered -> requireOwnedBy(registered, customerId).getId())
                .orElseGet(() -> register(customerId, vehicle));
    }

    @Override
    public Optional<Long> findIdByLicensePlate(LicensePlate licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate).map(Vehicle::getId);
    }

    private Long register(Long customerId, VehicleDetails vehicle) {
        CreateVehicleCommand command = new CreateVehicleCommand(
                customerId,
                vehicle.licensePlate(),
                null,
                vehicle.brand(),
                vehicle.model(),
                vehicle.year(),
                vehicle.color());
        return createVehicleUseCase.execute(command).id();
    }

    private static Vehicle requireOwnedBy(Vehicle vehicle, Long customerId) {
        if (!vehicle.getCustomerId().equals(customerId)) {
            throw new ConflictException(
                    "vehicle.licensePlateOfAnotherCustomer", vehicle.getLicensePlate().value());
        }
        return vehicle;
    }
}
