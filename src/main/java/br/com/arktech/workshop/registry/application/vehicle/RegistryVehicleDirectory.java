package br.com.arktech.workshop.registry.application.vehicle;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RegistryVehicleDirectory implements VehicleDirectory {

    private final VehicleRepository vehicleRepository;

    @Override
    public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
        return vehicleRepository
                .findByLicensePlate(vehicle.licensePlate())
                .map(registered -> requireOwnedBy(registered, customerId))
                .orElseGet(() -> register(customerId, vehicle))
                .getId();
    }

    private Vehicle register(Long customerId, VehicleDetails vehicle) {
        return vehicleRepository.save(new Vehicle(
                customerId,
                vehicle.licensePlate(),
                null,
                vehicle.brand(),
                vehicle.model(),
                vehicle.year(),
                vehicle.color()));
    }

    private static Vehicle requireOwnedBy(Vehicle vehicle, Long customerId) {
        if (!vehicle.getCustomerId().equals(customerId)) {
            throw new ConflictException(
                    "vehicle.licensePlateOfAnotherCustomer", vehicle.getLicensePlate().value());
        }
        return vehicle;
    }
}
