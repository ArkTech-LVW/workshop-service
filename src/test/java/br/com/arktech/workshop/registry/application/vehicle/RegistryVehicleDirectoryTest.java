package br.com.arktech.workshop.registry.application.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleUseCase;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.junit.jupiter.api.Test;

class RegistryVehicleDirectoryTest {

    private static final LicensePlate PLATE = new LicensePlate("ABC1D23");

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final RegistryVehicleDirectory vehicleDirectory = new RegistryVehicleDirectory(
            vehicleRepository, new CreateVehicleUseCase(vehicleRepository, customerRepository));

    private final Long anaId = saveCustomer("52998224725").getId();
    private final Long biaId = saveCustomer("12345678909").getId();

    @Test
    void shouldRegisterAVehicleWithAnUnknownPlateForTheCustomer() {
        Long id = vehicleDirectory.findOrRegister(
                anaId, new VehicleDetails("abc-1d23", "Volkswagen", "Gol", 2020, "Prata"));

        assertThat(vehicleRepository.findAll()).singleElement().satisfies(vehicle -> {
            assertThat(vehicle.getId()).isEqualTo(id);
            assertThat(vehicle.getCustomerId()).isEqualTo(anaId);
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
        Vehicle gol = vehicleRepository.save(new Vehicle(anaId, PLATE, null, "Volkswagen", "Gol", 2020, "Prata"));

        Long id = vehicleDirectory.findOrRegister(
                anaId, new VehicleDetails("ABC-1D23", "Fiat", "Uno", 2010, "Branco"));

        assertThat(id).isEqualTo(gol.getId());
        assertThat(vehicleRepository.findAll()).containsExactly(gol);
        assertThat(gol.getModel()).isEqualTo("Gol");
        assertThat(gol.getColor()).isEqualTo("Prata");
    }

    @Test
    void shouldRefuseAPlateRegisteredForAnotherCustomer() {
        Vehicle gol = vehicleRepository.save(new Vehicle(anaId, PLATE, null, "Volkswagen", "Gol", 2020, "Prata"));

        assertThatThrownBy(() -> vehicleDirectory.findOrRegister(
                        biaId, new VehicleDetails("ABC1D23", "Volkswagen", "Gol", 2020, "Prata")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.licensePlateOfAnotherCustomer");
        assertThat(vehicleRepository.findAll()).containsExactly(gol);
    }

    @Test
    void shouldRegisterOnlyForACustomerThatExists() {
        assertThatThrownBy(() -> vehicleDirectory.findOrRegister(
                        Long.MAX_VALUE, new VehicleDetails("ABC1D23", "Volkswagen", "Gol", 2020, "Prata")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
        assertThat(vehicleRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectAnInvalidPlateWithoutRegisteringAnything() {
        assertThatThrownBy(() -> vehicleDirectory.findOrRegister(
                        anaId, new VehicleDetails("AB-12", "Volkswagen", "Gol", 2020, "Prata")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("licensePlate.invalid");
        assertThat(vehicleRepository.findAll()).isEmpty();
    }

    @Test
    void shouldGiveTheDetailsOfARegisteredVehicle() {
        Vehicle gol = vehicleRepository.save(new Vehicle(anaId, PLATE, null, "Volkswagen", "Gol", 2020, "Prata"));

        assertThat(vehicleDirectory.detailsOf(gol.getId()))
                .isEqualTo(new VehicleDetails("ABC1D23", "Volkswagen", "Gol", 2020, "Prata"));
    }

    @Test
    void shouldReportAnUnknownVehicle() {
        assertThatThrownBy(() -> vehicleDirectory.detailsOf(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("vehicle.notFound");
    }

    private Customer saveCustomer(String document) {
        return customerRepository.save(
                new Customer("Cliente", CPFCNPJ.of(document), new Email("cliente@email.com"), null));
    }
}
