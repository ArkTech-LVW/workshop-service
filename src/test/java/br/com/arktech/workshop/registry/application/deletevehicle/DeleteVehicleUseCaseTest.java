package br.com.arktech.workshop.registry.application.deletevehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.junit.jupiter.api.Test;

class DeleteVehicleUseCaseTest {

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final DeleteVehicleUseCase deleteVehicleUseCase = new DeleteVehicleUseCase(vehicleRepository);

    @Test
    void shouldDeleteVehicle() {
        Vehicle vehicle = vehicleRepository.save(
                new Vehicle(1L, new LicensePlate("ABC1234"), null, "Fiat", "Uno", 2010, "Branco"));

        deleteVehicleUseCase.execute(new DeleteVehicleCommand(vehicle.getId()));

        assertThat(vehicleRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReportUnknownVehicle() {
        assertThatThrownBy(() -> deleteVehicleUseCase.execute(new DeleteVehicleCommand(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("vehicle.notFound");
    }
}
