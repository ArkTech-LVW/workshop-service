package br.com.arktech.workshop.serviceorder.application.deliverserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeliverServiceOrderUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long MECHANIC_ID = 5L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final DeliverServiceOrderUseCase useCase =
            new DeliverServiceOrderUseCase(serviceOrderRepository, () -> ATTENDANT_ID);

    private ServiceOrder serviceOrder;

    @BeforeEach
    void startTheExecution() {
        serviceOrder = serviceOrderRepository.save(
                new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));
        serviceOrder.addPartItem(40L, 1, new Money(new BigDecimal("35.90")));
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.sendQuote(MECHANIC_ID, null, null);
        serviceOrder.approveQuote(null, null, Instant.now());
        serviceOrder.startExecution(MECHANIC_ID, null);
    }

    @Test
    void shouldDeliverACompletedServiceOrderRecordingWhoDelivered() {
        serviceOrder.complete(MECHANIC_ID, null);

        ServiceOrderResult result = useCase.execute(new DeliverServiceOrderCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.DELIVERED);
        assertThat(serviceOrder.getStatusHistory()).last().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
            assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
            assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
        });
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotCompleted() {
        assertThatThrownBy(() -> useCase.execute(new DeliverServiceOrderCommand(serviceOrder.getId())))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }
}
