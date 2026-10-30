package br.com.arktech.workshop.serviceorder.application.startexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StartExecutionUseCaseTest {

    private static final Long MECHANIC_ID = 5L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final StartExecutionUseCase startExecutionUseCase =
            new StartExecutionUseCase(serviceOrderRepository, () -> MECHANIC_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldStartTheExecutionOfAnApprovedQuoteRecordingWhoStarted() {
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.sendQuote(MECHANIC_ID, null);
        serviceOrder.approveQuote(null, null);

        ServiceOrderResult result = startExecutionUseCase.execute(new StartExecutionCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(serviceOrder.getStatusHistory()).last().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.QUOTE_APPROVED);
            assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
            assertThat(change.getChangedByUserId()).isEqualTo(MECHANIC_ID);
            assertThat(change.getDescription()).isNull();
        });
    }

    @Test
    void shouldRefuseAServiceOrderWhoseQuoteWasNotApproved() {
        assertThatThrownBy(() -> startExecutionUseCase.execute(new StartExecutionCommand(serviceOrder.getId())))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
    }
}
