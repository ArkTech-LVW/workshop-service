package br.com.arktech.workshop.serviceorder.application.approvequoteascustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ApproveQuoteAsCustomerUseCaseTest {

    private static final Long ANA_ID = 10L;
    private static final String ANA_DOCUMENT = "529.982.247-25";
    private static final String BIA_DOCUMENT = "123.456.789-09";
    private static final CurrentUser NO_USER = new CurrentUser() {
        @Override
        public Long id() {
            throw new IllegalStateException("No authenticated user");
        }

        @Override
        public Optional<Long> findId() {
            return Optional.empty();
        }
    };

    private final List<Object> publishedEvents = new ArrayList<>();
    private final InMemoryServiceOrderRepository serviceOrderRepository =
            new InMemoryServiceOrderRepository(publishedEvents::add);
    private final Map<CPFCNPJ, Long> customers = Map.of(CPFCNPJ.of(ANA_DOCUMENT), ANA_ID, CPFCNPJ.of(BIA_DOCUMENT), 11L);
    private final CustomerDirectory customerDirectory = document -> Optional.ofNullable(customers.get(document));
    private final ApproveQuoteAsCustomerUseCase approveQuoteAsCustomerUseCase = new ApproveQuoteAsCustomerUseCase(
            new CustomerServiceOrderFinder(serviceOrderRepository, customerDirectory),
            new ApproveQuoteUseCase(serviceOrderRepository, NO_USER, Clock.systemUTC()));

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), ANA_ID, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldApproveTheQuoteWithoutAUserAndPublishThePartsToWithdraw() {
        sendQuoteWithParts();

        approveQuoteAsCustomerUseCase.execute(
                new ApproveQuoteAsCustomerCommand(serviceOrder.getPublicId(), ANA_DOCUMENT));

        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.QUOTE_APPROVED);
        assertThat(serviceOrder.getStatusHistory().getLast().getChangedByUserId()).isNull();
        assertThat(publishedEvents)
                .containsExactly(new QuoteApprovedEvent(
                        serviceOrder.getId(),
                        serviceOrder.getPublicId(),
                        serviceOrder.getCustomerId(),
                        serviceOrder.getTotalPrice().amount(),
                        List.of(new QuoteApprovedEvent.PartQuantity(40L, 3)),
                        null));
    }

    @Test
    void shouldNotApproveTheQuoteOfAnotherCustomer() {
        sendQuoteWithParts();

        assertThatThrownBy(() -> approveQuoteAsCustomerUseCase.execute(
                        new ApproveQuoteAsCustomerCommand(serviceOrder.getPublicId(), BIA_DOCUMENT)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(publishedEvents).isEmpty();
    }

    private void sendQuoteWithParts() {
        serviceOrder.startDiagnosis(5L, null);
        serviceOrder.addPartItem(40L, 3, new Money(new BigDecimal("35.90")));
        serviceOrder.sendQuote(7L, null, null);
        serviceOrder.clearDomainEvents();
    }
}
