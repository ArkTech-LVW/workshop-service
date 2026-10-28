package br.com.arktech.workshop.registry.application.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.junit.jupiter.api.Test;

class RegistryVehicleDirectoryTest {

    private static final Long ANA_ID = 1L;
    private static final Long BIA_ID = 2L;
    private static final LicensePlate PLATE = new LicensePlate("ABC1D23");

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final RegistryVehicleDirectory vehicleDirectory = new RegistryVehicleDirectory(vehicleRepository);

    @Test
    void shouldRegisterAVehicleWithAnUnknownPlateForTheCustomer() {
        Long id =
                vehicleDirectory.findOrRegister(ANA_ID, new VehicleDetails(PLATE, "Volkswagen", "Gol", 2020, "Prata"));

        assertThat(vehicleRepository.findAll()).singleElement().satisfies(vehicle -> {
            assertThat(vehicle.getId()).isEqualTo(id);
            assertThat(vehicle.getCustomerId()).isEqualTo(ANA_ID);
            assertThat(vehicle.getLicensePlate()).isEqualTo(PLATE);
            assertThat(vehicle.getChassis()).isNull();
            assertThat(vehicle.getBrand()).isEqualTo("Volkswagen");
            assertThat(vehicle.getModel()).isEqualTo("Gol");
            assertThat(vehicle.getYear()).isEqualTo(2020);
            assertThat(vehicle.getColor()).isEqualTo("Prata");
        });
    }

    @Test
    void shouldReuseTheVehicleTheCustomerAlreadyHasWithoutChangingIt() {
        Vehicle gol = vehicleRepository.save(new Vehicle(ANA_ID, PLATE, null, "Volkswagen", "Gol", 2020, "Prata"));

        Long id = vehicleDirectory.findOrRegister(ANA_ID, new VehicleDetails(PLATE, "Fiat", "Uno", 2010, "Branco"));

        assertThat(id).isEqualTo(gol.getId());
        assertThat(vehicleRepository.findAll()).containsExactly(gol);
        assertThat(gol.getModel()).isEqualTo("Gol");
        assertThat(gol.getColor()).isEqualTo("Prata");
    }

    @Test
    void shouldRefuseAPlateRegisteredForAnotherCustomer() {
        Vehicle gol = vehicleRepository.save(new Vehicle(ANA_ID, PLATE, null, "Volkswagen", "Gol", 2020, "Prata"));

        assertThatThrownBy(() -> vehicleDirectory.findOrRegister(
                        BIA_ID, new VehicleDetails(PLATE, "Volkswagen", "Gol", 2020, "Prata")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.licensePlateOfAnotherCustomer");
        assertThat(vehicleRepository.findAll()).containsExactly(gol);
    }
}
