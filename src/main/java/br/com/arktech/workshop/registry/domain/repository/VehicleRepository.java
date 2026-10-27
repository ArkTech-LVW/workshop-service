package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.registry.domain.model.Vehicle;

import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.Optional;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(Long id);

    boolean existsByLicensePlate(LicensePlate licensePlate);

    boolean existsByChassis(Chassis chassis);

    Page<Vehicle> findPage(VehicleFilter filter, Pagination pagination);

    default Vehicle findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("vehicle.notFound", id.toString()));
    }
}
