package br.com.arktech.workshop.shared.application.registry;

import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.Optional;

public interface VehicleDirectory {

    Long findOrRegister(Long customerId, VehicleDetails vehicle);

    VehicleDetails detailsOf(Long vehicleId);

    Optional<Long> findIdByLicensePlate(LicensePlate licensePlate);
}
