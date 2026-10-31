package br.com.arktech.workshop.serviceorder.application.findserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.StatusChangeResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FindServiceOrderUseCaseTest {

    private static final Long ATTENDANT_ID = 7L;
    private static final Long MECHANIC_ID = 8L;

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final FindServiceOrderUseCase useCase = new FindServiceOrderUseCase(serviceOrderRepository);

    @Test
    void shouldDetailItemsQuoteAndStatusHistory() {
        ServiceOrder serviceOrder = new ServiceOrder(
                serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", ATTENDANT_ID);
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(40L, 2, new Money(new BigDecimal("35.90")));
        serviceOrder.startDiagnosis(MECHANIC_ID, "Pastilhas gastas");
        Long id = serviceOrderRepository.saveAndFlush(serviceOrder).getId();

        ServiceOrderResult result = useCase.execute(new FindServiceOrderQuery(id));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(result.services()).singleElement().satisfies(item -> assertThat(item.serviceOfferingId())
                .isEqualTo(30L));
        assertThat(result.parts()).singleElement().satisfies(item -> assertThat(item.partId())
                .isEqualTo(40L));
        assertThat(result.totalPrice()).isEqualByComparingTo("221.80");
        assertThat(result.statusHistory())
                .extracting(
                        StatusChangeResult::fromStatus,
                        StatusChangeResult::toStatus,
                        StatusChangeResult::changedByUserId,
                        StatusChangeResult::description)
                .containsExactly(
                        tuple(null, ServiceOrderStatus.RECEIVED, ATTENDANT_ID, null),
                        tuple(
                                ServiceOrderStatus.RECEIVED,
                                ServiceOrderStatus.IN_DIAGNOSIS,
                                MECHANIC_ID,
                                "Pastilhas gastas"));
    }

    @Test
    void shouldReportUnknownServiceOrder() {
        assertThatThrownBy(() -> useCase.execute(new FindServiceOrderQuery(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }
}
