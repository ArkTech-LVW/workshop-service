package br.com.arktech.workshop.serviceorder.application.completeserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class CompleteServiceOrderUseCaseTest {

    private static final Long MECHANIC_ID = 5L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final CompleteServiceOrderUseCase useCase =
            new CompleteServiceOrderUseCase(serviceOrderRepository, () -> MECHANIC_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldCompleteAServiceOrderWithPartsOnly() {
        serviceOrder.addPartItem(40L, 1, new Money(new BigDecimal("35.90")));
        startTheExecution();

        ServiceOrderResult result = useCase.execute(new CompleteServiceOrderCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(serviceOrder.getStatusHistory()).last().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
            assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
            assertThat(change.getChangedByUserId()).isEqualTo(MECHANIC_ID);
        });
    }

    @Test
    void shouldRefuseAServiceOrderWithPendingServices() {
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        startTheExecution();

        assertThatThrownBy(() -> useCase.execute(new CompleteServiceOrderCommand(serviceOrder.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.pendingServiceItems");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    private void startTheExecution() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.sendQuote(MECHANIC_ID, null, null);
        serviceOrder.approveQuote(null, null, Instant.now());
        serviceOrder.startExecution(MECHANIC_ID, null);
    }
}
