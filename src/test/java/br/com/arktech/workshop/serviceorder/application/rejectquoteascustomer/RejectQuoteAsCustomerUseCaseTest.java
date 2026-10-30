package br.com.arktech.workshop.serviceorder.application.rejectquoteascustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteUseCase;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RejectQuoteAsCustomerUseCaseTest {

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

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final Map<CPFCNPJ, Long> customers = Map.of(CPFCNPJ.of(ANA_DOCUMENT), ANA_ID, CPFCNPJ.of(BIA_DOCUMENT), 11L);
    private final CustomerDirectory customerDirectory = document -> Optional.ofNullable(customers.get(document));
    private final RejectQuoteAsCustomerUseCase rejectQuoteAsCustomerUseCase = new RejectQuoteAsCustomerUseCase(
            new CustomerServiceOrderFinder(serviceOrderRepository, customerDirectory),
            new RejectQuoteUseCase(serviceOrderRepository, NO_USER));

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), ANA_ID, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldRejectTheQuoteWithoutAUserRecordingTheReason() {
        sendQuote();

        rejectQuoteAsCustomerUseCase.execute(
                new RejectQuoteAsCustomerCommand(serviceOrder.getPublicId(), ANA_DOCUMENT, "Achei o valor alto"));

        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.QUOTE_REJECTED);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getChangedByUserId()).isNull();
                    assertThat(change.getDescription()).isEqualTo("Achei o valor alto");
                });
    }

    @Test
    void shouldNotRejectTheQuoteOfAnotherCustomer() {
        sendQuote();

        assertThatThrownBy(() -> rejectQuoteAsCustomerUseCase.execute(
                        new RejectQuoteAsCustomerCommand(serviceOrder.getPublicId(), BIA_DOCUMENT, "Caro")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    private void sendQuote() {
        serviceOrder.startDiagnosis(5L, null);
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.sendQuote(7L, null);
    }
}
