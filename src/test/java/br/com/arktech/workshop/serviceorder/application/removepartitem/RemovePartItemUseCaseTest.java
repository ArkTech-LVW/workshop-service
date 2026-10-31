package br.com.arktech.workshop.serviceorder.application.removepartitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.PartItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RemovePartItemUseCaseTest {

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final RemovePartItemUseCase removePartItemUseCase = new RemovePartItemUseCase(serviceOrderRepository);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldRemoveThePartAndRecalculateTheTotal() {
        serviceOrder.addPartItem(40L, 2, new Money(new BigDecimal("35.90")));
        serviceOrder.addPartItem(41L, 1, new Money(new BigDecimal("12.00")));
        serviceOrderRepository.saveAndFlush(serviceOrder);
        Long padItemId = serviceOrder.getPartItems().getFirst().getId();

        removePartItemUseCase.execute(new RemovePartItemCommand(serviceOrder.getId(), padItemId));

        assertThat(serviceOrder.getPartItems()).singleElement().extracting(PartItem::getPartId).isEqualTo(41L);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("12.00")));
    }

    @Test
    void shouldRefuseToRemoveOnceTheQuoteIsSent() {
        serviceOrder.addPartItem(40L, 2, new Money(new BigDecimal("35.90")));
        serviceOrderRepository.saveAndFlush(serviceOrder);
        Long padItemId = serviceOrder.getPartItems().getFirst().getId();
        serviceOrder.startDiagnosis(2L, null);
        serviceOrder.sendQuote(2L, null, null);

        assertThatThrownBy(() ->
                        removePartItemUseCase.execute(new RemovePartItemCommand(serviceOrder.getId(), padItemId)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThat(serviceOrder.getPartItems()).hasSize(1);
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> removePartItemUseCase.execute(new RemovePartItemCommand(99L, 1L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }
}
