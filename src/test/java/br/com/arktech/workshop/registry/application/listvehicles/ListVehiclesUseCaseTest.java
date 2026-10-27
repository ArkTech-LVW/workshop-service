package br.com.arktech.workshop.registry.application.listvehicles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.InMemoryVehicleRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.junit.jupiter.api.Test;

class ListVehiclesUseCaseTest {

    private final InMemoryVehicleRepository vehicleRepository = new InMemoryVehicleRepository();
    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final ListVehiclesUseCase listVehiclesUseCase =
            new ListVehiclesUseCase(vehicleRepository, customerRepository);

    private final Customer ana = saveCustomer("52998224725");
    private final Customer bia = saveCustomer("12345678909");

    @Test
    void shouldListVehiclesOrderedByLicensePlate() {
        saveVehicle(ana, "XYZ9876");
        saveVehicle(bia, "ABC1234");

        Page<VehicleResult> page = listVehiclesUseCase.execute(new ListVehiclesQuery(null, null, 0, 20));

        assertThat(page.content()).extracting(VehicleResult::licensePlate).containsExactly("ABC1234", "XYZ9876");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldFilterByNormalizedLicensePlate() {
        saveVehicle(ana, "XYZ9876");
        saveVehicle(ana, "ABC1234");

        Page<VehicleResult> page = listVehiclesUseCase.execute(new ListVehiclesQuery("abc-1234", null, 0, 20));

        assertThat(page.content()).extracting(VehicleResult::licensePlate).containsExactly("ABC1234");
    }

    @Test
    void shouldListVehiclesOfCustomer() {
        saveVehicle(ana, "XYZ9876");
        saveVehicle(bia, "ABC1234");

        Page<VehicleResult> page = listVehiclesUseCase.execute(new ListVehiclesQuery(null, ana.getId(), 0, 20));

        assertThat(page.content()).extracting(VehicleResult::licensePlate).containsExactly("XYZ9876");
    }

    @Test
    void shouldReturnEmptyPageForCustomerWithoutVehicles() {
        Page<VehicleResult> page = listVehiclesUseCase.execute(new ListVehiclesQuery(null, bia.getId(), 0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void shouldReportUnknownCustomer() {
        assertThatThrownBy(() -> listVehiclesUseCase.execute(new ListVehiclesQuery(null, 1234L, 0, 20)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
    }

    private Customer saveCustomer(String document) {
        return customerRepository.save(
                new Customer("Cliente", CPFCNPJ.of(document), new Email("cliente@email.com"), null));
    }

    private void saveVehicle(Customer customer, String licensePlate) {
        vehicleRepository.save(
                new Vehicle(customer.getId(), new LicensePlate(licensePlate), null, "Fiat", "Uno", 2010, "Branco"));
    }
}
