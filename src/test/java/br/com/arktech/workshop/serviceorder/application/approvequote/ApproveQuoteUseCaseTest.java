package br.com.arktech.workshop.serviceorder.application.approvequote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteApproved;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.application.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ApproveQuoteUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long MECHANIC_ID = 5L;
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

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldApproveTheQuoteRecordingWhoApprovedIt() {
        sendQuoteWithParts();

        ServiceOrderResult result = useCase(() -> ATTENDANT_ID).execute(new ApproveQuoteCommand(serviceOrder.getId()));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.QUOTE_APPROVED);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
                    assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.QUOTE_APPROVED);
                    assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
                });
    }

    @Test
    void shouldPublishThePartsToWithdraw() {
        sendQuoteWithParts();

        useCase(() -> ATTENDANT_ID).execute(new ApproveQuoteCommand(serviceOrder.getId()));

        assertThat(publishedEvents)
                .containsExactly(
                        new QuoteApproved(serviceOrder.getPublicId()),
                        new QuoteApprovedEvent(
                                serviceOrder.getId(),
                                serviceOrder.getPublicId(),
                                List.of(
                                        new QuoteApprovedEvent.PartQuantity(40L, 3),
                                        new QuoteApprovedEvent.PartQuantity(41L, 1)),
                                ATTENDANT_ID));
    }

    @Test
    void shouldApproveWithoutLoggedUserWhenTheCustomerApproves() {
        sendQuoteWithParts();

        useCase(NO_USER).execute(new ApproveQuoteCommand(serviceOrder.getId()));

        assertThat(serviceOrder.getStatusHistory().getLast().getChangedByUserId()).isNull();
        assertThat(publishedEvents)
                .last()
                .isInstanceOfSatisfying(
                        QuoteApprovedEvent.class, event -> assertThat(event.approvedByUserId()).isNull());
    }

    @Test
    void shouldRefuseToApproveTwiceWithoutPublishing() {
        sendQuoteWithParts();
        useCase(() -> ATTENDANT_ID).execute(new ApproveQuoteCommand(serviceOrder.getId()));
        publishedEvents.clear();

        assertThatThrownBy(() -> useCase(() -> ATTENDANT_ID).execute(new ApproveQuoteCommand(serviceOrder.getId())))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> useCase(() -> ATTENDANT_ID).execute(new ApproveQuoteCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
        assertThat(publishedEvents).isEmpty();
    }

    private ApproveQuoteUseCase useCase(CurrentUser currentUser) {
        return new ApproveQuoteUseCase(serviceOrderRepository, currentUser, publishedEvents::add);
    }

    private void sendQuoteWithParts() {
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(40L, 3, new Money(new BigDecimal("35.90")));
        serviceOrder.addPartItem(41L, 1, new Money(new BigDecimal("80.00")));
        serviceOrder.sendQuote(ATTENDANT_ID, null);
        serviceOrder.clearDomainEvents();
    }
}
