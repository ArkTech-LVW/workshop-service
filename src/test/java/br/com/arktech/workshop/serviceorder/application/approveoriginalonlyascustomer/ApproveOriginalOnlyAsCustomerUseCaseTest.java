package br.com.arktech.workshop.serviceorder.application.approveoriginalonlyascustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.approveoriginalonly.ApproveOriginalOnlyUseCase;
import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class ApproveOriginalOnlyAsCustomerUseCaseTest {

    private static final Long ANA_ID = 10L;
    private static final String ANA_DOCUMENT = "529.982.247-25";
    private static final String BIA_DOCUMENT = "123.456.789-09";
    private static final Locale PT_BR = Locale.of("pt", "BR");
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
    private final ApproveOriginalOnlyAsCustomerUseCase approveOriginalOnlyAsCustomerUseCase =
            new ApproveOriginalOnlyAsCustomerUseCase(
                    new CustomerServiceOrderFinder(serviceOrderRepository, customerDirectory),
                    new ApproveOriginalOnlyUseCase(serviceOrderRepository, NO_USER, messageResolver()));

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), ANA_ID, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldApproveOnlyTheOriginalQuoteWithoutAUser() {
        sendSupplementaryQuote();

        approveOriginalOnlyAsCustomerUseCase.execute(
                new ApproveOriginalOnlyAsCustomerCommand(serviceOrder.getPublicId(), ANA_DOCUMENT));

        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.SUPPLEMENTARY_APPROVED);
        assertThat(serviceOrder.getPartItems()).hasSize(1);
        assertThat(serviceOrder.getStatusHistory().getLast().getChangedByUserId()).isNull();
    }

    @Test
    void shouldNotApproveTheQuoteOfAnotherCustomer() {
        sendSupplementaryQuote();

        assertThatThrownBy(() -> approveOriginalOnlyAsCustomerUseCase.execute(
                        new ApproveOriginalOnlyAsCustomerCommand(serviceOrder.getPublicId(), BIA_DOCUMENT)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_SUPPLEMENTARY_APPROVAL);
    }

    private void sendSupplementaryQuote() {
        serviceOrder.startDiagnosis(5L, null);
        serviceOrder.addPartItem(40L, 3, new Money(new BigDecimal("35.90")));
        serviceOrder.sendQuote(7L, null, null);
        serviceOrder.approveQuote(null, null, Instant.parse("2026-10-28T10:00:00Z"));
        serviceOrder.startExecution(5L, null);
        serviceOrder.addPartItem(41L, 1, new Money(new BigDecimal("38.00")));
        serviceOrder.sendQuote(7L, true, null);
    }

    private static MessageResolver messageResolver() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("serviceOrder.originalOnlyApproved", PT_BR, "Aprovado apenas o orçamento original.");
        return new MessageResolver(messageSource, PT_BR);
    }
}
