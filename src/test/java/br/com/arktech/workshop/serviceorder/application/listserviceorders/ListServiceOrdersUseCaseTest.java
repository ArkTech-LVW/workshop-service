package br.com.arktech.workshop.serviceorder.application.listserviceorders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderSummaryResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ListServiceOrdersUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long ANA_ID = 10L;
    private static final Long BIA_ID = 11L;
    private static final Long GOL_ID = 20L;
    private static final Long UNO_ID = 21L;
    private static final String ANA_DOCUMENT = "529.982.247-25";
    private static final String GOL_PLATE = "abc-1d23";

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final Map<CPFCNPJ, Long> customers = Map.of(CPFCNPJ.of(ANA_DOCUMENT), ANA_ID);
    private final CustomerDirectory customerDirectory = document -> Optional.ofNullable(customers.get(document));
    private final Map<LicensePlate, Long> vehicles = Map.of(new LicensePlate(GOL_PLATE), GOL_ID);
    private final VehicleDirectory vehicleDirectory = new VehicleDirectory() {

        @Override
        public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> findIdByLicensePlate(LicensePlate licensePlate) {
            return Optional.ofNullable(vehicles.get(licensePlate));
        }
    };
    private final ListServiceOrdersUseCase useCase =
            new ListServiceOrdersUseCase(serviceOrderRepository, customerDirectory, vehicleDirectory);

    private final ServiceOrder anasGol = save(ANA_ID, GOL_ID);
    private final ServiceOrder anasUno = save(ANA_ID, UNO_ID);
    private final ServiceOrder biasUno = save(BIA_ID, UNO_ID);

    @Test
    void shouldListEveryServiceOrderWithoutFilters() {
        Page<ServiceOrderSummaryResult> page = useCase.execute(query(null, null, null));

        assertThat(page.content())
                .extracting(ServiceOrderSummaryResult::id)
                .containsExactly(anasGol.getId(), anasUno.getId(), biasUno.getId());
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void shouldFilterByTheCustomerOfTheDocument() {
        Page<ServiceOrderSummaryResult> page = useCase.execute(query(null, "52998224725", null));

        assertThat(page.content())
                .extracting(ServiceOrderSummaryResult::id)
                .containsExactly(anasGol.getId(), anasUno.getId());
    }

    @Test
    void shouldFilterByTheVehicleOfThePlateAndByStatus() {
        anasGol.cancel(ATTENDANT_ID, "Cliente desistiu");

        assertThat(useCase.execute(query(null, null, "ABC1D23")).content())
                .extracting(ServiceOrderSummaryResult::id)
                .containsExactly(anasGol.getId());
        assertThat(useCase.execute(query(ServiceOrderStatus.RECEIVED, null, GOL_PLATE)).content())
                .isEmpty();
    }

    @Test
    void shouldAnswerAnEmptyPageForAnUnknownDocumentOrPlate() {
        Page<ServiceOrderSummaryResult> unknownCustomer = useCase.execute(query(null, "12345678909", null));
        Page<ServiceOrderSummaryResult> unknownVehicle = useCase.execute(query(null, ANA_DOCUMENT, "XYZ9Z99"));

        assertThat(unknownCustomer.content()).isEmpty();
        assertThat(unknownCustomer.totalElements()).isZero();
        assertThat(unknownVehicle.content()).isEmpty();
        assertThat(unknownVehicle.totalElements()).isZero();
    }

    @Test
    void shouldSummarizeWithoutTheItems() {
        ServiceOrderSummaryResult summary = useCase.execute(query(null, null, GOL_PLATE))
                .content()
                .getFirst();

        assertThat(summary.id()).isEqualTo(anasGol.getId());
        assertThat(summary.publicId()).isEqualTo(anasGol.getPublicId());
        assertThat(summary.number()).isEqualTo(anasGol.getNumber().value());
        assertThat(summary.customerId()).isEqualTo(ANA_ID);
        assertThat(summary.vehicleId()).isEqualTo(GOL_ID);
        assertThat(summary.mechanicId()).isNull();
        assertThat(summary.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(summary.totalPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectAnInvalidDocument() {
        assertThatThrownBy(() -> useCase.execute(query(null, "123", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("cpfCnpj.invalid");
    }

    private ServiceOrder save(Long customerId, Long vehicleId) {
        return serviceOrderRepository.save(new ServiceOrder(
                serviceOrderRepository.nextNumber(), customerId, vehicleId, "Barulho ao frear", ATTENDANT_ID));
    }

    private static ListServiceOrdersQuery query(
            @Nullable ServiceOrderStatus status, @Nullable String customerDocument, @Nullable String licensePlate) {
        return new ListServiceOrdersQuery(status, customerDocument, licensePlate, null, null, 0, 20);
    }
}
