package br.com.arktech.workshop.serviceorder.application.reopenquote;

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

class ReopenQuoteUseCaseTest {

    private static final Long ADMIN_ID = 3L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final ReopenQuoteUseCase reopenQuoteUseCase =
            new ReopenQuoteUseCase(serviceOrderRepository, () -> ADMIN_ID);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldReopenARejectedQuoteForANewOne() {
        serviceOrder.startDiagnosis(5L, null);
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.sendQuote(7L, null);
        serviceOrder.rejectQuote(7L, "Cliente achou o valor alto");

        ServiceOrderResult result = reopenQuoteUseCase.execute(
                new ReopenQuoteCommand(serviceOrder.getId(), "Cliente pediu peça mais barata"));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.QUOTE_REJECTED);
                    assertThat(change.getChangedByUserId()).isEqualTo(ADMIN_ID);
                    assertThat(change.getDescription()).isEqualTo("Cliente pediu peça mais barata");
                });
    }

    @Test
    void shouldRefuseAServiceOrderWhoseQuoteWasNotRejected() {
        assertThatThrownBy(() -> reopenQuoteUseCase.execute(new ReopenQuoteCommand(serviceOrder.getId(), null)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
    }
}
