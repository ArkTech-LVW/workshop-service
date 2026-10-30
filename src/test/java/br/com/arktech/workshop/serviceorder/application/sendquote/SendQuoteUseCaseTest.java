package br.com.arktech.workshop.serviceorder.application.sendquote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SendQuoteUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long MECHANIC_ID = 5L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final SendQuoteUseCase sendQuoteUseCase =
            new SendQuoteUseCase(serviceOrderRepository, () -> ATTENDANT_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldSendTheQuoteRecordingWhoSentIt() {
        diagnoseWithAService();

        ServiceOrderResult result = sendQuoteUseCase.execute(new SendQuoteCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(result.totalPrice()).isEqualTo(new BigDecimal("300.00"));
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
                    assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
                    assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
                });
    }

    @Test
    void shouldLockTheItemsAfterSending() {
        diagnoseWithAService();

        sendQuoteUseCase.execute(new SendQuoteCommand(serviceOrder.getId()));

        assertThatThrownBy(() -> serviceOrder.addPartItem(40L, 1, new Money(new BigDecimal("35.90"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
    }

    @Test
    void shouldRefuseAQuoteWithoutItems() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);

        assertThatThrownBy(() -> sendQuoteUseCase.execute(new SendQuoteCommand(serviceOrder.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.quoteWithoutItems");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotInDiagnosis() {
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));

        assertThatThrownBy(() -> sendQuoteUseCase.execute(new SendQuoteCommand(serviceOrder.getId())))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(serviceOrder.getStatusHistory())
                .extracting(StatusChange::getToStatus)
                .containsExactly(ServiceOrderStatus.RECEIVED);
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> sendQuoteUseCase.execute(new SendQuoteCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }

    private void diagnoseWithAService() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.addServiceItem(30L, 2, new Money(new BigDecimal("150.00")));
    }
}
