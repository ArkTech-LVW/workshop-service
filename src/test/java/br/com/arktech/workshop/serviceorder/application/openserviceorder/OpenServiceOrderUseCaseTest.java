package br.com.arktech.workshop.serviceorder.application.openserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

class OpenServiceOrderUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long ANA_ID = 10L;
    private static final Long GOL_ID = 20L;
    private static final VehicleDetails GOL = new VehicleDetails("abc-1d23", "Volkswagen", "Gol", 2020, "Prata");

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final Map<CPFCNPJ, Long> customers = Map.of(CPFCNPJ.of("52998224725"), ANA_ID);
    private final CustomerDirectory customerDirectory = document -> Optional.ofNullable(customers.get(document));
    private final List<VehicleDetails> requestedVehicles = new ArrayList<>();
    private final VehicleDirectory vehicleDirectory = vehicleDirectory((customerId, vehicle) -> {
        requestedVehicles.add(vehicle);
        return GOL_ID;
    });

    @Test
    void shouldOpenAReceivedServiceOrderForTheCustomerAndTheirVehicle() {
        ServiceOrderResult result = useCase(vehicleDirectory)
                .execute(new OpenServiceOrderCommand("529.982.247-25", GOL, "Barulho ao frear"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.publicId()).isNotNull();
        assertThat(result.number()).isEqualTo("SO-" + Year.now() + "-000001");
        assertThat(result.customerId()).isEqualTo(ANA_ID);
        assertThat(result.vehicleId()).isEqualTo(GOL_ID);
        assertThat(result.mechanicId()).isNull();
        assertThat(result.problemDescription()).isEqualTo("Barulho ao frear");
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(requestedVehicles).containsExactly(GOL);
        assertThat(serviceOrderRepository.findAll())
                .singleElement()
                .satisfies(serviceOrder -> assertThat(serviceOrder.getStatusHistory())
                        .extracting(StatusChange::getChangedByUserId)
                        .containsExactly(ATTENDANT_ID));
    }

    @Test
    void shouldNumberEachServiceOrderInSequence() {
        OpenServiceOrderUseCase openServiceOrderUseCase = useCase(vehicleDirectory);
        OpenServiceOrderCommand command = new OpenServiceOrderCommand("52998224725", GOL, "Revisão");

        String first = openServiceOrderUseCase.execute(command).number();
        String second = openServiceOrderUseCase.execute(command).number();

        assertThat(first).endsWith("-000001");
        assertThat(second).endsWith("-000002");
    }

    @Test
    void shouldAskToRegisterACustomerThatIsNotRegisteredYet() {
        assertThatThrownBy(() -> useCase(vehicleDirectory)
                        .execute(new OpenServiceOrderCommand("12345678909", GOL, "Barulho ao frear")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.customerNotRegistered");
        assertThat(requestedVehicles).isEmpty();
        assertThat(serviceOrderRepository.findAll()).isEmpty();
    }

    @Test
    void shouldNotOpenWhenThePlateBelongsToAnotherCustomer() {
        VehicleDirectory otherCustomersVehicle = vehicleDirectory((customerId, vehicle) -> {
            throw new ConflictException("vehicle.licensePlateOfAnotherCustomer", vehicle.licensePlate());
        });

        assertThatThrownBy(() -> useCase(otherCustomersVehicle)
                        .execute(new OpenServiceOrderCommand("52998224725", GOL, "Barulho ao frear")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("vehicle.licensePlateOfAnotherCustomer");
        assertThat(serviceOrderRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectAnInvalidDocumentBeforeLookingAnythingUp() {
        assertThatThrownBy(() -> useCase(vehicleDirectory)
                        .execute(new OpenServiceOrderCommand("123", GOL, "Barulho ao frear")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("cpfCnpj.invalid");
        assertThat(requestedVehicles).isEmpty();
        assertThat(serviceOrderRepository.findAll()).isEmpty();
    }

    private OpenServiceOrderUseCase useCase(VehicleDirectory vehicleDirectory) {
        return new OpenServiceOrderUseCase(
                serviceOrderRepository, customerDirectory, vehicleDirectory, () -> ATTENDANT_ID);
    }

    private static VehicleDirectory vehicleDirectory(BiFunction<Long, VehicleDetails, Long> findOrRegister) {
        return new VehicleDirectory() {

            @Override
            public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
                return findOrRegister.apply(customerId, vehicle);
            }

            @Override
            public Optional<Long> findIdByLicensePlate(LicensePlate licensePlate) {
                throw new UnsupportedOperationException();
            }

            @Override
            public VehicleDetails detailsOf(Long vehicleId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
