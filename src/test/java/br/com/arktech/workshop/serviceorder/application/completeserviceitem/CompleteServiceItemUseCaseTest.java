package br.com.arktech.workshop.serviceorder.application.completeserviceitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteServiceItemUseCaseTest {

    private static final Long MECHANIC_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-10-28T16:45:00Z");

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final CompleteServiceItemUseCase useCase = new CompleteServiceItemUseCase(
            serviceOrderRepository, () -> MECHANIC_ID, Clock.fixed(NOW, ZoneId.systemDefault()));

    private ServiceOrder serviceOrder;
    private Long firstItemId;
    private Long secondItemId;

    @BeforeEach
    void startTheExecutionWithTwoServices() {
        serviceOrder = serviceOrderRepository.save(
                new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addServiceItem(31L, 1, new Money(new BigDecimal("80.00")));
        serviceOrderRepository.saveAndFlush(serviceOrder);
        serviceOrder.startDiagnosis(MECHANIC_ID, null);
        serviceOrder.sendQuote(MECHANIC_ID, null, null);
        serviceOrder.approveQuote(null, null, Instant.now());
        serviceOrder.startExecution(MECHANIC_ID, null);
        serviceOrder.clearDomainEvents();
        firstItemId = serviceOrder.getServiceItems().getFirst().getId();
        secondItemId = serviceOrder.getServiceItems().getLast().getId();
    }

    @Test
    void shouldCompleteTheServiceAtTheCurrentTimeByTheLoggedUser() {
        ServiceOrderResult result =
                useCase.execute(new CompleteServiceItemCommand(serviceOrder.getId(), firstItemId));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(result.services().getFirst().completedAt()).isEqualTo(NOW);
        assertThat(result.services().getLast().completedAt()).isNull();
        assertThat(serviceOrder.getServiceItems().getFirst().getCompletedByUserId()).isEqualTo(MECHANIC_ID);
    }

    @Test
    void shouldCompleteTheServiceOrderWithTheLastService() {
        useCase.execute(new CompleteServiceItemCommand(serviceOrder.getId(), firstItemId));

        ServiceOrderResult result =
                useCase.execute(new CompleteServiceItemCommand(serviceOrder.getId(), secondItemId));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(result.services()).allSatisfy(item -> assertThat(item.completedAt()).isEqualTo(NOW));
        assertThat(serviceOrder.getStatusHistory()).last().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
            assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
            assertThat(change.getChangedByUserId()).isEqualTo(MECHANIC_ID);
        });
    }

    @Test
    void shouldRefuseToCompleteTheSameServiceTwice() {
        useCase.execute(new CompleteServiceItemCommand(serviceOrder.getId(), firstItemId));

        assertThatThrownBy(() -> useCase.execute(new CompleteServiceItemCommand(serviceOrder.getId(), firstItemId)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.serviceItemAlreadyCompleted");
    }
}
