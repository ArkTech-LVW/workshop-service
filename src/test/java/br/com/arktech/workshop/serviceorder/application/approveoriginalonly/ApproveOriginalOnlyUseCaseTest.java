package br.com.arktech.workshop.serviceorder.application.approveoriginalonly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class ApproveOriginalOnlyUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long MECHANIC_ID = 5L;
    private static final Long PAD_ID = 40L;
    private static final Long OIL_ID = 41L;
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String ORIGINAL_ONLY_APPROVED = "Aprovado apenas o orçamento original.";

    private final List<Object> publishedEvents = new ArrayList<>();
    private final InMemoryServiceOrderRepository serviceOrderRepository =
            new InMemoryServiceOrderRepository(publishedEvents::add);
    private final ApproveOriginalOnlyUseCase approveOriginalOnlyUseCase =
            new ApproveOriginalOnlyUseCase(serviceOrderRepository, () -> ATTENDANT_ID, messageResolver());

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldKeepOnlyTheApprovedItemsAndRecordTheAnswer() {
        sendSupplementaryQuote(true);

        ServiceOrderResult result =
                approveOriginalOnlyUseCase.execute(new ApproveOriginalOnlyCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.SUPPLEMENTARY_APPROVED);
        assertThat(result.parts()).singleElement().satisfies(part -> assertThat(part.partId()).isEqualTo(PAD_ID));
        assertThat(result.totalPrice()).isEqualByComparingTo("71.80");
        assertThat(serviceOrder.getStatusHistory().getLast()).satisfies(change -> {
            assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
            assertThat(change.getDescription()).isEqualTo(ORIGINAL_ONLY_APPROVED);
        });
        assertThat(publishedEvents)
                .containsExactly(new QuoteApprovedEvent(
                        serviceOrder.getId(),
                        serviceOrder.getPublicId(),
                        serviceOrder.getCustomerId(),
                        serviceOrder.getTotalPrice().amount(),
                        List.of(new QuoteApprovedEvent.PartQuantity(PAD_ID, 2)),
                        ATTENDANT_ID));
    }

    @Test
    void shouldNotApproveOnlyTheOriginalQuoteWhenTheWorkshopDidNotAllowIt() {
        sendSupplementaryQuote(false);

        assertThatThrownBy(() ->
                        approveOriginalOnlyUseCase.execute(new ApproveOriginalOnlyCommand(serviceOrder.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.originalOnlyNotAllowed");
        assertThat(serviceOrder.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_SUPPLEMENTARY_APPROVAL);
        assertThat(publishedEvents).isEmpty();
    }

    private void sendSupplementaryQuote(boolean canProceedWithoutChanges) {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.addPartItem(PAD_ID, 2, new Money(new BigDecimal("35.90")));
        serviceOrder.sendQuote(ATTENDANT_ID, null, null);
        serviceOrder.approveQuote(null, null, Instant.parse("2026-10-28T10:00:00Z"));
        serviceOrder.startExecution(MECHANIC_ID, null);
        serviceOrder.addPartItem(OIL_ID, 4, new Money(new BigDecimal("38.00")));
        serviceOrder.sendQuote(ATTENDANT_ID, canProceedWithoutChanges, null);
        serviceOrder.clearDomainEvents();
    }

    private static MessageResolver messageResolver() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("serviceOrder.originalOnlyApproved", PT_BR, ORIGINAL_ONLY_APPROVED);
        return new MessageResolver(messageSource, PT_BR);
    }
}
