package br.com.arktech.workshop.registry.application.findvehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.junit.jupiter.api.Test;

class FindVehicleUseCaseTest {

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final FindVehicleUseCase findVehicleUseCase = new FindVehicleUseCase(vehicleRepository);

    @Test
    void shouldFindVehicle() {
        Vehicle vehicle = vehicleRepository.save(
                new Vehicle(1L, new LicensePlate("ABC1234"), null, "Fiat", "Uno", 2010, "Branco"));

        VehicleResult result = findVehicleUseCase.execute(new FindVehicleQuery(vehicle.getId()));

        assertThat(result)
                .isEqualTo(new VehicleResult(vehicle.getId(), 1L, "ABC1234", null, "Fiat", "Uno", 2010, "Branco"));
    }

    @Test
    void shouldReportUnknownVehicle() {
        assertThatThrownBy(() -> findVehicleUseCase.execute(new FindVehicleQuery(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("vehicle.notFound");
    }
}
