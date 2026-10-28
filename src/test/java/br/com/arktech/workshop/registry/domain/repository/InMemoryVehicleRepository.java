package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.ArrayList;
import java.util.Comparator;
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
    public Optional<Vehicle> findByLicensePlate(LicensePlate licensePlate) {
        return vehicles.stream().filter(vehicle -> vehicle.getLicensePlate().equals(licensePlate)).findFirst();
    }

    @Override
    public boolean existsByLicensePlate(LicensePlate licensePlate) {
        return vehicles.stream().anyMatch(vehicle -> vehicle.getLicensePlate().equals(licensePlate));
    }

    @Override
    public boolean existsByLicensePlateAndIdNot(LicensePlate licensePlate, Long id) {
        return vehicles.stream()
                .anyMatch(vehicle -> vehicle.getLicensePlate().equals(licensePlate) && !vehicle.getId().equals(id));
    }

    @Override
    public boolean existsByChassis(Chassis chassis) {
        return vehicles.stream().anyMatch(vehicle -> chassis.equals(vehicle.getChassis()));
    }

    @Override
    public boolean existsByChassisAndIdNot(Chassis chassis, Long id) {
        return vehicles.stream()
                .anyMatch(vehicle -> chassis.equals(vehicle.getChassis()) && !vehicle.getId().equals(id));
    }

    @Override
    public Page<Vehicle> findPage(VehicleFilter filter, Pagination pagination) {
        List<Vehicle> matching = vehicles.stream()
                .filter(vehicle ->
                        filter.licensePlate() == null || vehicle.getLicensePlate().equals(filter.licensePlate()))
                .filter(vehicle -> filter.customerId() == null || vehicle.getCustomerId().equals(filter.customerId()))
                .sorted(Comparator.comparing((Vehicle vehicle) -> vehicle.getLicensePlate().value())
                        .thenComparing(Vehicle::getId))
                .toList();
        List<Vehicle> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    @Override
    public void remove(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    public List<Vehicle> findAll() {
        return List.copyOf(vehicles);
    }
}
