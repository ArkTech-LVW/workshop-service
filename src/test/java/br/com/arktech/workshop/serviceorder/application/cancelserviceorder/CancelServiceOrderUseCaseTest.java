package br.com.arktech.workshop.serviceorder.application.cancelserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.model.StatusChange;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCanceledEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelServiceOrderUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final String REASON = "Cliente desistiu do serviço";

    private final List<Object> publishedEvents = new ArrayList<>();
    private final InMemoryServiceOrderRepository serviceOrderRepository =
            new InMemoryServiceOrderRepository(publishedEvents::add);
    private final CancelServiceOrderUseCase useCase =
            new CancelServiceOrderUseCase(serviceOrderRepository, () -> ATTENDANT_ID);

    private ServiceOrder serviceOrder;

    @BeforeEach
    void openAServiceOrder() {
        serviceOrder = serviceOrderRepository.save(
                new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));
    }

    @Test
    void shouldCancelRecordingTheReasonAndWhoCanceled() {
        ServiceOrderResult result = useCase.execute(new CancelServiceOrderCommand(serviceOrder.getId(), REASON));

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.CANCELED);
        assertThat(serviceOrder.getStatusHistory())
                .last()
                .satisfies(change -> {
                    assertThat(change.getFromStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
                    assertThat(change.getToStatus()).isEqualTo(ServiceOrderStatus.CANCELED);
                    assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
                    assertThat(change.getDescription()).isEqualTo(REASON);
                });
        assertThat(publishedEvents)
                .containsExactly(
                        new ServiceOrderCanceledEvent(serviceOrder.getId(), serviceOrder.getPublicId(), ATTENDANT_ID));
    }

    @Test
    void shouldRefuseToCancelTwice() {
        useCase.execute(new CancelServiceOrderCommand(serviceOrder.getId(), REASON));

        assertThatThrownBy(() -> useCase.execute(new CancelServiceOrderCommand(serviceOrder.getId(), REASON)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(serviceOrder.getStatusHistory())
                .extracting(StatusChange::getToStatus)
                .containsExactly(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.CANCELED);
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownServiceOrder() {
        assertThatThrownBy(() -> useCase.execute(new CancelServiceOrderCommand(99L, REASON)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }
}
