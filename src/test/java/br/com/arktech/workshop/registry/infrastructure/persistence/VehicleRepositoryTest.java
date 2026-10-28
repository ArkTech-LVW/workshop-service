package br.com.arktech.workshop.registry.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.VehicleFilter;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import jakarta.persistence.EntityManager;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Customer ana;

    private Customer bia;

    @BeforeEach
    void saveCustomers() {
        ana = customerRepository.save(newCustomer("52998224725"));
        bia = customerRepository.save(newCustomer("12345678909"));
    }

    @Test
    void shouldPersistPlateChassisDataAndTimestamps() {
        Vehicle saved = vehicleRepository.save(new Vehicle(
                ana.getId(),
                new LicensePlate("abc-1d23"),
                new Chassis("9bwzzz372vt004251"),
                "Volkswagen",
                "Gol",
                2020,
                "Prata"));
        flushAndClear();

        Vehicle found = vehicleRepository.findRequiredById(saved.getId());

        assertThat(found.getCustomerId()).isEqualTo(ana.getId());
        assertThat(found.getLicensePlate()).isEqualTo(new LicensePlate("ABC1D23"));
        assertThat(found.getChassis()).isEqualTo(new Chassis("9BWZZZ372VT004251"));
        assertThat(found.getBrand()).isEqualTo("Volkswagen");
        assertThat(found.getModel()).isEqualTo("Gol");
        assertThat(found.getYear()).isEqualTo(2020);
        assertThat(found.getColor()).isEqualTo("Prata");
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getLastUpdated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(columnsOf(saved))
                .containsEntry("license_plate", "ABC1D23")
                .containsEntry("chassis", "9BWZZZ372VT004251");
    }

    @Test
    void shouldPersistVehicleWithoutChassis() {
        Vehicle saved = vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();

        assertThat(vehicleRepository.findRequiredById(saved.getId()).getChassis()).isNull();
    }

    @Test
    void shouldCheckWhetherLicensePlateAndChassisAreRegistered() {
        vehicleRepository.save(newVehicle(ana, "ABC1234", "1HGCM82633A004352"));
        flushAndClear();

        assertThat(vehicleRepository.existsByLicensePlate(new LicensePlate("ABC1234"))).isTrue();
        assertThat(vehicleRepository.existsByLicensePlate(new LicensePlate("XYZ9876"))).isFalse();
        assertThat(vehicleRepository.existsByChassis(new Chassis("1HGCM82633A004352"))).isTrue();
        assertThat(vehicleRepository.existsByChassis(new Chassis("1M8GDM9AXKP042788"))).isFalse();
    }

    @Test
    void shouldFindVehicleByLicensePlate() {
        Vehicle saved = vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();

        assertThat(vehicleRepository.findByLicensePlate(new LicensePlate("abc-1234")))
                .get()
                .extracting(Vehicle::getId)
                .isEqualTo(saved.getId());
        assertThat(vehicleRepository.findByLicensePlate(new LicensePlate("XYZ9876"))).isEmpty();
    }

    @Test
    void shouldCheckLicensePlateAndChassisIgnoringItsOwnVehicle() {
        Vehicle saved = vehicleRepository.save(newVehicle(ana, "ABC1234", "1HGCM82633A004352"));
        flushAndClear();
        LicensePlate licensePlate = new LicensePlate("ABC1234");
        Chassis chassis = new Chassis("1HGCM82633A004352");

        assertThat(vehicleRepository.existsByLicensePlateAndIdNot(licensePlate, saved.getId())).isFalse();
        assertThat(vehicleRepository.existsByLicensePlateAndIdNot(licensePlate, saved.getId() + 1)).isTrue();
        assertThat(vehicleRepository.existsByChassisAndIdNot(chassis, saved.getId())).isFalse();
        assertThat(vehicleRepository.existsByChassisAndIdNot(chassis, saved.getId() + 1)).isTrue();
    }

    @Test
    void shouldFilterByLicensePlateAndCustomerOrderedByPlate() {
        vehicleRepository.save(newVehicle(ana, "ZZZ9C99", null));
        vehicleRepository.save(newVehicle(ana, "ZZZ9A99", null));
        vehicleRepository.save(newVehicle(bia, "ZZZ9B99", null));
        flushAndClear();

        Page<Vehicle> byCustomer =
                vehicleRepository.findPage(new VehicleFilter(null, ana.getId()), new Pagination(0, 20));
        Page<Vehicle> byPlate = vehicleRepository.findPage(
                new VehicleFilter(new LicensePlate("ZZZ9B99"), null), new Pagination(0, 20));
        Page<Vehicle> byBoth = vehicleRepository.findPage(
                new VehicleFilter(new LicensePlate("ZZZ9B99"), ana.getId()), new Pagination(0, 20));

        assertThat(byCustomer.content())
                .extracting(vehicle -> vehicle.getLicensePlate().value())
                .containsExactly("ZZZ9A99", "ZZZ9C99");
        assertThat(byCustomer.totalElements()).isEqualTo(2);
        assertThat(byPlate.content()).extracting(Vehicle::getCustomerId).containsExactly(bia.getId());
        assertThat(byBoth.content()).isEmpty();
        assertThat(byBoth.totalElements()).isZero();
    }

    @Test
    void shouldAllowManyVehiclesWithoutChassis() {
        vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        vehicleRepository.save(newVehicle(ana, "XYZ9876", null));

        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicle WHERE customer_id = ?", Long.class, ana.getId()))
                .isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicatedLicensePlate() {
        vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();

        assertThatThrownBy(() -> {
                    vehicleRepository.save(newVehicle(bia, "ABC1234", null));
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicatedChassis() {
        vehicleRepository.save(newVehicle(ana, "ABC1234", "1HGCM82633A004352"));
        flushAndClear();

        assertThatThrownBy(() -> {
                    vehicleRepository.save(newVehicle(bia, "XYZ9876", "1HGCM82633A004352"));
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUnknownCustomer() {
        assertThatThrownBy(() -> {
                    vehicleRepository.save(new Vehicle(
                            Long.MAX_VALUE, new LicensePlate("ABC1234"), null, "Fiat", "Uno", 2010, "Branco"));
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldKeepCustomerThatHasVehicles() {
        vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();
        Customer customer = customerRepository.findRequiredById(ana.getId());

        assertThatThrownBy(() -> customerRepository.remove(customer))
                .isInstanceOf(ConflictException.class)
                .hasMessage("customer.inUse");
    }

    @Test
    void shouldRemoveVehicle() {
        Vehicle saved = vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();

        vehicleRepository.remove(vehicleRepository.findRequiredById(saved.getId()));
        flushAndClear();

        assertThat(vehicleRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldRefuseToRemoveReferencedVehicle() {
        Vehicle saved = vehicleRepository.save(newVehicle(ana, "ABC1234", null));
        flushAndClear();
        jdbcTemplate.execute("CREATE TABLE vehicle_reference (vehicle_id BIGINT REFERENCES vehicle (id))");
        jdbcTemplate.update("INSERT INTO vehicle_reference (vehicle_id) VALUES (?)", saved.getId());
        Vehicle vehicle = vehicleRepository.findRequiredById(saved.getId());

        assertThatThrownBy(() -> vehicleRepository.remove(vehicle))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.inUse");
    }

    @Test
    void shouldReportUnknownVehicle() {
        assertThatThrownBy(() -> vehicleRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("vehicle.notFound");
    }

    private static Customer newCustomer(String document) {
        return new Customer("Cliente", CPFCNPJ.of(document), new Email("cliente@email.com"), null);
    }

    private static Vehicle newVehicle(Customer customer, String licensePlate, @Nullable String chassis) {
        return new Vehicle(
                customer.getId(),
                new LicensePlate(licensePlate),
                chassis == null ? null : new Chassis(chassis),
                "Fiat",
                "Uno",
                2010,
                "Branco");
    }

    private Map<String, Object> columnsOf(Vehicle vehicle) {
        return jdbcTemplate.queryForMap(
                "SELECT license_plate, chassis FROM vehicle WHERE id = ?", vehicle.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
