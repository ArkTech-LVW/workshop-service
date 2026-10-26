package br.com.arktech.workshop.registry.application.updatevehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class UpdateVehicleUseCaseTest {

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final UpdateVehicleUseCase updateVehicleUseCase = new UpdateVehicleUseCase(vehicleRepository);

    @Test
    void shouldRegisterChassisLaterChangePlateAndColor() {
        Vehicle vehicle = saveVehicle("ABC1234", null);

        VehicleResult result = updateVehicleUseCase.execute(
                new UpdateVehicleCommand(vehicle.getId(), "abc-1d34", "9bwzzz372vt004251", "Azul"));

        assertThat(result)
                .isEqualTo(new VehicleResult(
                        vehicle.getId(), 1L, "ABC1D34", "9BWZZZ372VT004251", "Fiat", "Uno", 2010, "Azul"));
    }

    @Test
    void shouldClearChassis() {
        Vehicle vehicle = saveVehicle("ABC1234", "1HGCM82633A004352");

        VehicleResult result =
                updateVehicleUseCase.execute(new UpdateVehicleCommand(vehicle.getId(), "ABC1234", null, "Branco"));

        assertThat(result.chassis()).isNull();
    }

    @Test
    void shouldKeepItsOwnPlateAndChassis() {
        Vehicle vehicle = saveVehicle("ABC1234", "1HGCM82633A004352");

        VehicleResult result = updateVehicleUseCase.execute(
                new UpdateVehicleCommand(vehicle.getId(), "ABC1234", "1HGCM82633A004352", "Preto"));

        assertThat(result.color()).isEqualTo("Preto");
    }

    @Test
    void shouldRejectPlateOfAnotherVehicle() {
        Vehicle vehicle = saveVehicle("ABC1234", null);
        saveVehicle("XYZ9876", null);

        assertThatThrownBy(() -> updateVehicleUseCase.execute(
                        new UpdateVehicleCommand(vehicle.getId(), "XYZ-9876", null, "Branco")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.licensePlateAlreadyExists");
        assertThat(vehicle.getLicensePlate()).isEqualTo(new LicensePlate("ABC1234"));
    }

    @Test
    void shouldRejectChassisOfAnotherVehicle() {
        Vehicle vehicle = saveVehicle("ABC1234", null);
        saveVehicle("XYZ9876", "1HGCM82633A004352");

        assertThatThrownBy(() -> updateVehicleUseCase.execute(
                        new UpdateVehicleCommand(vehicle.getId(), "ABC1234", "1HGCM82633A004352", "Branco")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.chassisAlreadyExists");
        assertThat(vehicle.getChassis()).isNull();
    }

    @Test
    void shouldReportUnknownVehicle() {
        assertThatThrownBy(() ->
                        updateVehicleUseCase.execute(new UpdateVehicleCommand(1234L, "ABC1234", null, "Branco")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("vehicle.notFound");
    }

    private Vehicle saveVehicle(String licensePlate, @Nullable String chassis) {
        return vehicleRepository.save(new Vehicle(
                1L,
                new LicensePlate(licensePlate),
                chassis == null ? null : new Chassis(chassis),
                "Fiat",
                "Uno",
                2010,
                "Branco"));
    }
}
