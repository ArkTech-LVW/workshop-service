package br.com.arktech.workshop.registry.application.createvehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import org.junit.jupiter.api.Test;

class CreateVehicleUseCaseTest {

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final CreateVehicleUseCase createVehicleUseCase =
            new CreateVehicleUseCase(vehicleRepository, customerRepository);

    private final Customer ana = customerRepository.save(
            new Customer("Ana", CPFCNPJ.of("52998224725"), new Email("ana@email.com"), null));

    @Test
    void shouldCreateVehicleWithNormalizedPlateAndChassis() {
        VehicleResult result = createVehicleUseCase.execute(new CreateVehicleCommand(
                ana.getId(), "abc-1d23", "9bwzzz37-2vt004251", "Volkswagen", "Gol", 2020, "Prata"));

        assertThat(result)
                .isEqualTo(new VehicleResult(
                        1L, ana.getId(), "ABC1D23", "9BWZZZ372VT004251", "Volkswagen", "Gol", 2020, "Prata"));
        assertThat(vehicleRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldCreateVehicleWithoutChassis() {
        VehicleResult result = createVehicleUseCase.execute(
                new CreateVehicleCommand(ana.getId(), "ABC1234", null, "Fiat", "Uno", 2010, "Branco"));

        assertThat(result.chassis()).isNull();
    }

    @Test
    void shouldRequireExistingCustomer() {
        assertThatThrownBy(() -> createVehicleUseCase.execute(
                        new CreateVehicleCommand(1234L, "ABC1234", null, "Fiat", "Uno", 2010, "Branco")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
        assertThat(vehicleRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectDuplicatedLicensePlate() {
        createVehicleUseCase.execute(
                new CreateVehicleCommand(ana.getId(), "ABC1234", null, "Fiat", "Uno", 2010, "Branco"));

        assertThatThrownBy(() -> createVehicleUseCase.execute(
                        new CreateVehicleCommand(ana.getId(), "abc-1234", null, "Fiat", "Palio", 2012, "Preto")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.licensePlateAlreadyExists");
    }

    @Test
    void shouldRejectDuplicatedChassis() {
        createVehicleUseCase.execute(new CreateVehicleCommand(
                ana.getId(), "ABC1234", "1HGCM82633A004352", "Honda", "Accord", 2003, "Prata"));

        assertThatThrownBy(() -> createVehicleUseCase.execute(new CreateVehicleCommand(
                        ana.getId(), "XYZ9876", "1hgcm82633a004352", "Honda", "Accord", 2003, "Prata")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.chassisAlreadyExists");
    }

    @Test
    void shouldRejectYearOutOfRange() {
        assertThatThrownBy(() -> createVehicleUseCase.execute(
                        new CreateVehicleCommand(ana.getId(), "ABC1234", null, "Fiat", "147", 1980, "Branco")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("vehicle.invalidYear");
    }
}
