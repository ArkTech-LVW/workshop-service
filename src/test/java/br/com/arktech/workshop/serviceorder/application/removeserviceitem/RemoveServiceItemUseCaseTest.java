package br.com.arktech.workshop.serviceorder.application.removeserviceitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RemoveServiceItemUseCaseTest {

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final RemoveServiceItemUseCase removeServiceItemUseCase =
            new RemoveServiceItemUseCase(serviceOrderRepository);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldRemoveTheServiceAndRecalculateTheTotal() {
        serviceOrder.addServiceItem(30L, 1, new Money(new BigDecimal("150.00")));
        serviceOrder.addServiceItem(31L, 1, new Money(new BigDecimal("80.00")));
        serviceOrderRepository.saveAndFlush(serviceOrder);
        Long brakesItemId = serviceOrder.getServiceItems().getFirst().getId();

        removeServiceItemUseCase.execute(new RemoveServiceItemCommand(serviceOrder.getId(), brakesItemId));

        assertThat(serviceOrder.getServiceItems())
                .singleElement()
                .extracting(ServiceItem::getServiceOfferingId)
                .isEqualTo(31L);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("80.00")));
    }

    @Test
    void shouldReportAnItemThatIsNotInTheServiceOrder() {
        assertThatThrownBy(() ->
                        removeServiceItemUseCase.execute(new RemoveServiceItemCommand(serviceOrder.getId(), 99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.itemNotFound");
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> removeServiceItemUseCase.execute(new RemoveServiceItemCommand(99L, 1L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }
}
