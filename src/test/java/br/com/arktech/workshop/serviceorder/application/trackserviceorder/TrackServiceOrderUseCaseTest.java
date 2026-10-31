package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.catalog.ServiceOfferingDirectory;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.application.inventory.PartDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class TrackServiceOrderUseCaseTest {

    private static final Long ANA_ID = 10L;
    private static final Long GOL_ID = 20L;
    private static final Long MECHANIC_ID = 5L;
    private static final Long ATTENDANT_ID = 7L;
    private static final String ANA_DOCUMENT = "529.982.247-25";
    private static final Long BRAKES_ID = 30L;
    private static final Long PAD_ID = 40L;
    private static final Long OIL_ID = 41L;
    private static final VehicleDetails GOL = new VehicleDetails("ABC1D23", "Volkswagen", "Gol", 2020, "Prata");

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final TrackServiceOrderUseCase trackServiceOrderUseCase = new TrackServiceOrderUseCase(
            new CustomerServiceOrderFinder(
                    serviceOrderRepository,
                    document -> Optional.of(ANA_ID).filter(id -> document.equals(CPFCNPJ.of(ANA_DOCUMENT)))),
            new VehicleDirectory() {
                @Override
                public Long findOrRegister(Long customerId, VehicleDetails vehicle) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<Long> findIdByLicensePlate(LicensePlate licensePlate) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public VehicleDetails detailsOf(Long vehicleId) {
                    assertThat(vehicleId).isEqualTo(GOL_ID);
                    return GOL;
                }
            },
            new ServiceOfferingDirectory() {
                @Override
                public Money laborPriceOf(Long serviceOfferingId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Map<Long, String> namesOf(Collection<Long> serviceOfferingIds) {
                    return namesFrom(Map.of(BRAKES_ID, "Troca de pastilhas"), serviceOfferingIds);
                }
            },
            new PartDirectory() {
                @Override
                public PartDetails detailsOf(Long partId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Map<Long, String> namesOf(Collection<Long> partIds) {
                    return namesFrom(Map.of(PAD_ID, "Pastilha de freio", OIL_ID, "Óleo 5W30"), partIds);
                }

                @Override
                public void requireAvailableFor(Long serviceOrderId, Long partId, int quantity) {
                    throw new UnsupportedOperationException();
                }
            });

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), ANA_ID, GOL_ID, "Barulho ao frear", 1L));

    @Test
    void shouldShowTheStatusTheHistoryAndTheVehicle() {
        serviceOrder.startDiagnosis(MECHANIC_ID, "Pastilhas gastas");

        TrackServiceOrderResult result = track();

        assertThat(result.publicId()).isEqualTo(serviceOrder.getPublicId());
        assertThat(result.number()).isEqualTo(serviceOrder.getNumber().value());
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(result.problemDescription()).isEqualTo("Barulho ao frear");
        assertThat(result.vehicle()).isEqualTo(GOL);
        assertThat(result.statusHistory())
                .extracting(StatusChangeResult::status, StatusChangeResult::description)
                .containsExactly(
                        tuple(ServiceOrderStatus.RECEIVED, null),
                        tuple(ServiceOrderStatus.IN_DIAGNOSIS, "Pastilhas gastas"));
    }

    @Test
    void shouldHideTheQuoteBeforeItIsSent() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        addItems();

        assertThat(track().quote()).isNull();
    }

    @Test
    void shouldShowTheDetailedQuoteOnceItIsSent() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        addItems();
        serviceOrder.sendQuote(ATTENDANT_ID, null, null);

        QuoteResult quote = track().quote();

        assertThat(quote).isNotNull();
        assertThat(quote.services())
                .containsExactly(new QuoteItemResult(
                        "Troca de pastilhas",
                        1,
                        new BigDecimal("150.00"),
                        new BigDecimal("150.00"),
                        QuoteItemStage.PENDING));
        assertThat(quote.parts())
                .containsExactly(
                        new QuoteItemResult(
                                "Pastilha de freio",
                                2,
                                new BigDecimal("45.90"),
                                new BigDecimal("91.80"),
                                QuoteItemStage.PENDING),
                        new QuoteItemResult(
                                "Óleo 5W30", 4, new BigDecimal("38.00"), new BigDecimal("152.00"), QuoteItemStage.PENDING));
        assertThat(quote.servicesTotal()).isEqualByComparingTo("150.00");
        assertThat(quote.partsTotal()).isEqualByComparingTo("243.80");
        assertThat(quote.totalPrice()).isEqualByComparingTo("393.80");
        assertThat(quote.approvedTotal()).isEqualByComparingTo("0.00");
        assertThat(quote.pendingTotal()).isEqualByComparingTo("393.80");
        assertThat(quote.canProceedWithoutChanges()).isNull();
    }

    @Test
    void shouldSeparateWhatWasDoneApprovedAndAddedInTheSupplementaryQuote() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.addServiceItem(BRAKES_ID, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(PAD_ID, 2, new Money(new BigDecimal("45.90")));
        serviceOrderRepository.saveAndFlush(serviceOrder);
        serviceOrder.sendQuote(ATTENDANT_ID, null, null);
        serviceOrder.approveQuote(null, null, Instant.parse("2026-10-28T10:00:00Z"));
        serviceOrder.startExecution(MECHANIC_ID, null);
        serviceOrder.addPartItem(OIL_ID, 4, new Money(new BigDecimal("38.00")));
        serviceOrder.completeServiceItem(
                serviceOrder.getServiceItems().getFirst().getId(), MECHANIC_ID, Instant.parse("2026-10-28T12:00:00Z"));
        serviceOrder.sendQuote(ATTENDANT_ID, true, null);

        TrackServiceOrderResult result = track();
        QuoteResult quote = result.quote();

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.AWAITING_SUPPLEMENTARY_APPROVAL);
        assertThat(quote).isNotNull();
        assertThat(quote.services()).extracting(QuoteItemResult::stage).containsExactly(QuoteItemStage.COMPLETED);
        assertThat(quote.parts())
                .extracting(QuoteItemResult::name, QuoteItemResult::stage)
                .containsExactly(
                        tuple("Pastilha de freio", QuoteItemStage.APPROVED),
                        tuple("Óleo 5W30", QuoteItemStage.PENDING));
        assertThat(quote.approvedTotal()).isEqualByComparingTo("241.80");
        assertThat(quote.pendingTotal()).isEqualByComparingTo("152.00");
        assertThat(quote.canProceedWithoutChanges()).isTrue();
    }

    @Test
    void shouldHideTheQuoteAgainWhenItIsReopenedForChanges() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        addItems();
        serviceOrder.sendQuote(ATTENDANT_ID, null, null);
        serviceOrder.rejectQuote(null, "Achei caro");
        serviceOrder.reopenQuote(ATTENDANT_ID, null);

        assertThat(track().quote()).isNull();
    }

    @Test
    void shouldNotShowTheServiceOrderToAnotherCustomer() {
        assertThatThrownBy(() -> trackServiceOrderUseCase.execute(
                        new TrackServiceOrderQuery(serviceOrder.getPublicId(), "123.456.789-09")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }

    private TrackServiceOrderResult track() {
        return trackServiceOrderUseCase.execute(new TrackServiceOrderQuery(serviceOrder.getPublicId(), ANA_DOCUMENT));
    }

    private void addItems() {
        serviceOrder.addServiceItem(BRAKES_ID, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(PAD_ID, 2, new Money(new BigDecimal("45.90")));
        serviceOrder.addPartItem(OIL_ID, 4, new Money(new BigDecimal("38.00")));
    }

    private static Map<Long, String> namesFrom(Map<Long, String> names, Collection<Long> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(Function.identity(), names::get));
    }
}
