package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryVehicleRepository implements VehicleRepository {

    private final List<Vehicle> vehicles = new ArrayList<>();

    private long nextId = 1;

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (!vehicles.contains(vehicle)) {
            ReflectionTestUtils.setField(vehicle, "id", nextId++);
            vehicles.add(vehicle);
        }
        return vehicle;
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return vehicles.stream().filter(vehicle -> vehicle.getId().equals(id)).findFirst();
    }

    @Override
    public boolean existsByLicensePlate(LicensePlate licensePlate) {
        return vehicles.stream().anyMatch(vehicle -> vehicle.getLicensePlate().equals(licensePlate));
    }

    @Override
    public boolean existsByChassis(Chassis chassis) {
        return vehicles.stream().anyMatch(vehicle -> chassis.equals(vehicle.getChassis()));
    }

    public List<Vehicle> findAll() {
        return List.copyOf(vehicles);
    }
}
