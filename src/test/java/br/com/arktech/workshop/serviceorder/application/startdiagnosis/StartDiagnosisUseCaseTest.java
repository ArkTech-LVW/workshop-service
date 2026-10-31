package br.com.arktech.workshop.serviceorder.application.startdiagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.PartItemResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceItemResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StartDiagnosisUseCaseTest {

    private static final Long MECHANIC_ID = 5L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final StartDiagnosisUseCase startDiagnosisUseCase =
            new StartDiagnosisUseCase(serviceOrderRepository, () -> MECHANIC_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldStartTheDiagnosisWithTheLoggedUserAsTheMechanic() {
        ServiceOrderResult result = startDiagnosisUseCase.execute(new StartDiagnosisCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(result.mechanicId()).isEqualTo(MECHANIC_ID);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .extracting(StatusChange::getChangedByUserId)
                .isEqualTo(MECHANIC_ID);
    }

    @Test
    void shouldDetailTheQuoteItemByItem() {
        serviceOrder.addServiceItem(30L, 2, new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(40L, 3, new Money(new BigDecimal("35.90")));
        serviceOrderRepository.saveAndFlush(serviceOrder);

        ServiceOrderResult result = startDiagnosisUseCase.execute(new StartDiagnosisCommand(serviceOrder.getId()));

        assertThat(result.services())
                .singleElement()
                .isEqualTo(new ServiceItemResult(
                        serviceOrder.getServiceItems().getFirst().getId(),
                        30L,
                        2,
                        new BigDecimal("150.00"),
                        new BigDecimal("300.00"),
                        null,
                        null));
        assertThat(result.parts())
                .singleElement()
                .isEqualTo(new PartItemResult(
                        serviceOrder.getPartItems().getFirst().getId(),
                        40L,
                        3,
                        new BigDecimal("35.90"),
                        new BigDecimal("107.70"),
                        null));
        assertThat(result.servicesTotal()).isEqualTo(new BigDecimal("300.00"));
        assertThat(result.partsTotal()).isEqualTo(new BigDecimal("107.70"));
        assertThat(result.totalPrice()).isEqualTo(new BigDecimal("407.70"));
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotReceived() {
        serviceOrder.startDiagnosis(1L, null);

        assertThatThrownBy(() -> startDiagnosisUseCase.execute(new StartDiagnosisCommand(serviceOrder.getId())))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> startDiagnosisUseCase.execute(new StartDiagnosisCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }
}
