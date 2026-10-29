package br.com.arktech.workshop.serviceorder.application.rejectquote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RejectQuoteUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final RejectQuoteUseCase rejectQuoteUseCase =
            new RejectQuoteUseCase(serviceOrderRepository, () -> ATTENDANT_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldRejectTheQuoteRecordingTheReason() {
        sendQuote();

        ServiceOrderResult result = rejectQuoteUseCase.execute(
                new RejectQuoteCommand(serviceOrder.getId(), "Cliente achou o valor alto"));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.QUOTE_REJECTED);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.QUOTE_REJECTED);
                    assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
                    assertThat(change.getDescription()).isEqualTo("Cliente achou o valor alto");
                });
    }

    @Test
    void shouldRequireTheReason() {
        sendQuote();

        assertThatThrownBy(() -> rejectQuoteUseCase.execute(new RejectQuoteCommand(serviceOrder.getId(), " ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.reasonRequired");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void shouldRefuseAServiceOrderThatIsNotAwaitingApproval() {
        assertThatThrownBy(() -> rejectQuoteUseCase.execute(new RejectQuoteCommand(serviceOrder.getId(), "Caro")))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
    }

    private void sendQuote() {
        serviceOrder.startDiagnosis(5L, null);
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.sendQuote(ATTENDANT_ID, null);
    }
}
