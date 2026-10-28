package br.com.arktech.workshop.serviceorder.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ServiceOrderRepositoryTest {

    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));
    private static final Money PART_PRICE = new Money(new BigDecimal("35.90"));

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ServiceOrderJpaRepository serviceOrderJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private Long customerId;

    private Long vehicleId;

    private Long attendantId;

    private Long mechanicId;

    private Long serviceOfferingId;

    private Long partId;

    @BeforeEach
    void saveReferencedRecords() {
        Customer customer = customerRepository.save(
                new Customer("Cliente", CPFCNPJ.of("52998224725"), new Email("cliente@email.com"), null));
        customerId = customer.getId();
        vehicleId = vehicleRepository
                .save(new Vehicle(customerId, new LicensePlate("ABC1D23"), null, "Volkswagen", "Gol", 2020, "Prata"))
                .getId();
        attendantId = userRepository.save(newUser("atendente@workshop.com", Role.ATTENDANT)).getId();
        mechanicId = userRepository.save(newUser("mecanico@workshop.com", Role.MECHANIC)).getId();
        serviceOfferingId = serviceOfferingRepository
                .save(new ServiceOffering("Troca de pastilhas", null, LABOR_PRICE, 60))
                .getId();
        partId = partRepository
                .save(new Part(
                        new Sku("PAST-001"), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, PART_PRICE, 2))
                .getId();
    }

    @Test
    void shouldPersistTheWholeAggregate() {
        ServiceOrder serviceOrder = newServiceOrder(new ServiceOrderNumber("SO-2026-900001"));
        serviceOrder.addServiceItem(serviceOfferingId, 1, LABOR_PRICE);
        serviceOrder.addPartItem(partId, 4, PART_PRICE);
        serviceOrder.startDiagnosis(mechanicId, "Pastilhas gastas");
        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);
        flushAndClear();

        ServiceOrder found = serviceOrderRepository.findRequiredById(saved.getId());

        assertThat(found.getPublicId()).isEqualTo(serviceOrder.getPublicId());
        assertThat(found.getNumber()).isEqualTo(new ServiceOrderNumber("SO-2026-900001"));
        assertThat(found.getCustomerId()).isEqualTo(customerId);
        assertThat(found.getVehicleId()).isEqualTo(vehicleId);
        assertThat(found.getMechanicId()).isEqualTo(mechanicId);
        assertThat(found.getProblemDescription()).isEqualTo("Barulho ao frear");
        assertThat(found.getStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(found.getTotalPrice()).isEqualTo(new Money(new BigDecimal("293.60")));
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(found.getServiceItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isNotNull();
            assertThat(item.getServiceOfferingId()).isEqualTo(serviceOfferingId);
            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.getUnitPrice()).isEqualTo(LABOR_PRICE);
        });
        assertThat(found.getPartItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isNotNull();
            assertThat(item.getPartId()).isEqualTo(partId);
            assertThat(item.getQuantity()).isEqualTo(4);
            assertThat(item.getUnitPrice()).isEqualTo(PART_PRICE);
        });
        assertThat(found.getStatusHistory())
                .extracting(
                        StatusChange::getFromStatus,
                        StatusChange::getToStatus,
                        StatusChange::getChangedByUserId,
                        StatusChange::getDescription)
                .containsExactly(
                        tuple(null, ServiceOrderStatus.RECEIVED, attendantId, null),
                        tuple(
                                ServiceOrderStatus.RECEIVED,
                                ServiceOrderStatus.IN_DIAGNOSIS,
                                mechanicId,
                                "Pastilhas gastas"));
        assertThat(found.getStatusHistory()).allSatisfy(change -> assertThat(change.getDateCreated()).isNotNull());
    }

    @Test
    void shouldPersistItemRemovalAndNewStatusChanges() {
        ServiceOrder serviceOrder = newServiceOrder(new ServiceOrderNumber("SO-2026-900002"));
        serviceOrder.addServiceItem(serviceOfferingId, 1, LABOR_PRICE);
        serviceOrder.addPartItem(partId, 2, PART_PRICE);
        serviceOrder.startDiagnosis(mechanicId, null);
        Long id = serviceOrderRepository.save(serviceOrder).getId();
        flushAndClear();

        ServiceOrder found = serviceOrderRepository.findRequiredById(id);
        found.removePartItem(found.getPartItems().getFirst().getId());
        found.sendQuote(attendantId, null);
        found.approveQuote(null, null);
        serviceOrderRepository.save(found);
        flushAndClear();

        ServiceOrder updated = serviceOrderRepository.findRequiredById(id);
        assertThat(updated.getStatus()).isEqualTo(ServiceOrderStatus.QUOTE_APPROVED);
        assertThat(updated.getPartItems()).isEmpty();
        assertThat(updated.getServiceItems()).hasSize(1);
        assertThat(updated.getTotalPrice()).isEqualTo(LABOR_PRICE);
        assertThat(updated.getStatusHistory())
                .extracting(StatusChange::getToStatus, StatusChange::getChangedByUserId)
                .containsExactly(
                        tuple(ServiceOrderStatus.RECEIVED, attendantId),
                        tuple(ServiceOrderStatus.IN_DIAGNOSIS, mechanicId),
                        tuple(ServiceOrderStatus.AWAITING_APPROVAL, attendantId),
                        tuple(ServiceOrderStatus.QUOTE_APPROVED, null));
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldPersistTheCancellationReason() {
        Long id = serviceOrderRepository.save(newServiceOrder(new ServiceOrderNumber("SO-2026-900004"))).getId();
        flushAndClear();

        ServiceOrder found = serviceOrderRepository.findRequiredById(id);
        found.cancel(attendantId, "Cliente desistiu do reparo");
        serviceOrderRepository.save(found);
        flushAndClear();

        ServiceOrder canceled = serviceOrderRepository.findRequiredById(id);
        assertThat(canceled.getStatus()).isEqualTo(ServiceOrderStatus.CANCELED);
        assertThat(canceled.getStatusHistory().getLast()).satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
            assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.CANCELED);
            assertThat(change.getChangedByUserId()).isEqualTo(attendantId);
            assertThat(change.getDescription()).isEqualTo("Cliente desistiu do reparo");
        });
    }

    @Test
    void shouldGenerateSequentialNumbersForTheCurrentYearInSaoPaulo() {
        ServiceOrderNumber first = serviceOrderRepository.nextNumber();
        ServiceOrderNumber second = serviceOrderRepository.nextNumber();

        String prefix = "SO-" + Year.now(ZoneId.of("America/Sao_Paulo")) + "-";
        assertThat(first.value()).startsWith(prefix);
        assertThat(second.value()).startsWith(prefix);
        assertThat(sequenceOf(second)).isEqualTo(sequenceOf(first) + 1);
    }

    @Test
    void shouldStartTheCountOverInEachYear() {
        assertThat(serviceOrderJpaRepository.incrementNumberCounter(1999)).isEqualTo(1);
        assertThat(serviceOrderJpaRepository.incrementNumberCounter(1999)).isEqualTo(2);
        assertThat(serviceOrderJpaRepository.incrementNumberCounter(2000)).isEqualTo(1);
        assertThat(serviceOrderJpaRepository.incrementNumberCounter(1999)).isEqualTo(3);
    }

    @Test
    void shouldReportUnknownServiceOrder() {
        assertThatThrownBy(() -> serviceOrderRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }

    @Test
    void shouldStoreTheHighestTotalPriceTheAggregateAccepts() {
        Money highest = new Money(new BigDecimal("9999999999.99"));
        ServiceOrder serviceOrder = newServiceOrder(new ServiceOrderNumber("SO-2026-900005"));
        serviceOrder.addServiceItem(serviceOfferingId, 1, highest);
        Long id = serviceOrderRepository.save(serviceOrder).getId();
        flushAndClear();

        assertThat(serviceOrderRepository.findRequiredById(id).getTotalPrice()).isEqualTo(highest);
    }

    @Test
    void shouldRejectDuplicatedNumber() {
        serviceOrderRepository.save(newServiceOrder(new ServiceOrderNumber("SO-2026-900003")));
        flushAndClear();

        assertThatThrownBy(() -> {
            serviceOrderRepository.save(newServiceOrder(new ServiceOrderNumber("SO-2026-900003")));
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ServiceOrder newServiceOrder(ServiceOrderNumber number) {
        return new ServiceOrder(number, customerId, vehicleId, "Barulho ao frear", attendantId);
    }

    private User newUser(String email, Role role) {
        return new User("Funcionário", new Email(email), "secret", Set.of(role), passwordHasher);
    }

    private static long sequenceOf(ServiceOrderNumber number) {
        return Long.parseLong(number.value().substring(number.value().lastIndexOf('-') + 1));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
