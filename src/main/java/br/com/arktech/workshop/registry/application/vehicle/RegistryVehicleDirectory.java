package br.com.arktech.workshop.registry.application.vehicle;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RegistryVehicleDirectory implements VehicleDirectory {

    private final VehicleRepository vehicleRepository;

    @Override
    public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
        LicensePlate licensePlate = new LicensePlate(vehicle.licensePlate());
        return vehicleRepository
                .findByLicensePlate(licensePlate)
                .map(registered -> requireOwnedBy(registered, customerId))
                .orElseGet(() -> register(customerId, licensePlate, vehicle))
                .getId();
    }

    private Vehicle register(Long customerId, LicensePlate licensePlate, VehicleDetails vehicle) {
        return vehicleRepository.save(new Vehicle(
                customerId,
                licensePlate,
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
