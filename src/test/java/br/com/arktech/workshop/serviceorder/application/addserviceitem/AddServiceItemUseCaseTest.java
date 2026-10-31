package br.com.arktech.workshop.serviceorder.application.addserviceitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.catalog.ServiceOfferingDirectory;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class AddServiceItemUseCaseTest {

    private static final Long BRAKES_ID = 30L;
    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final List<Long> pricedServices = new ArrayList<>();
    private final ServiceOfferingDirectory serviceOfferingDirectory = pricing(serviceOfferingId -> {
        pricedServices.add(serviceOfferingId);
        return LABOR_PRICE;
    });
    private final AddServiceItemUseCase addServiceItemUseCase =
            new AddServiceItemUseCase(serviceOrderRepository, serviceOfferingDirectory);

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    @Test
    void shouldAddTheServiceWithItsCurrentLaborPrice() {
        AddServiceItemResult result =
                addServiceItemUseCase.execute(new AddServiceItemCommand(serviceOrder.getId(), BRAKES_ID, 2));

        assertThat(result.id()).isNotNull();
        assertThat(result.serviceOfferingId()).isEqualTo(BRAKES_ID);
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.unitPrice()).isEqualByComparingTo("150.00");
        assertThat(result.subtotal()).isEqualByComparingTo("300.00");
        assertThat(result.serviceOrderTotalPrice()).isEqualByComparingTo("300.00");
        assertThat(serviceOrder.getServiceItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(result.id());
            assertThat(item.getUnitPrice()).isEqualTo(LABOR_PRICE);
        });
        assertThat(pricedServices).containsExactly(BRAKES_ID);
    }

    @Test
    void shouldAnswerWithTheNewItemAndTheTotalOfAllItems() {
        AddServiceItemResult first =
                addServiceItemUseCase.execute(new AddServiceItemCommand(serviceOrder.getId(), BRAKES_ID, 1));

        AddServiceItemResult second =
                addServiceItemUseCase.execute(new AddServiceItemCommand(serviceOrder.getId(), BRAKES_ID + 1, 2));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.serviceOfferingId()).isEqualTo(BRAKES_ID + 1);
        assertThat(second.serviceOrderTotalPrice()).isEqualByComparingTo("450.00");
        assertThat(serviceOrder.getServiceItems()).hasSize(2);
    }

    @Test
    void shouldReportAnUnknownServiceOrderBeforePricingTheService() {
        assertThatThrownBy(() -> addServiceItemUseCase.execute(new AddServiceItemCommand(99L, BRAKES_ID, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
        assertThat(pricedServices).isEmpty();
    }

    @Test
    void shouldNotAddAServiceTheCatalogRefuses() {
        AddServiceItemUseCase withInactiveService = new AddServiceItemUseCase(serviceOrderRepository, pricing(id -> {
            throw new BusinessRuleException("serviceOffering.inactive", id.toString());
        }));

        assertThatThrownBy(() ->
                        withInactiveService.execute(new AddServiceItemCommand(serviceOrder.getId(), BRAKES_ID, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.inactive");
        assertThat(serviceOrder.getServiceItems()).isEmpty();
    }

    @Test
    void shouldRefuseItemsOnceTheQuoteIsSent() {
        serviceOrder.startDiagnosis(2L, null);
        serviceOrder.addPartItem(40L, 1, new Money(new BigDecimal("35.90")));
        serviceOrder.sendQuote(2L, null, null);

        assertThatThrownBy(() ->
                        addServiceItemUseCase.execute(new AddServiceItemCommand(serviceOrder.getId(), BRAKES_ID, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThat(serviceOrder.getServiceItems()).isEmpty();
    }

    private static ServiceOfferingDirectory pricing(Function<Long, Money> laborPriceOf) {
        return new ServiceOfferingDirectory() {
            @Override
            public Money laborPriceOf(Long serviceOfferingId) {
                return laborPriceOf.apply(serviceOfferingId);
            }

            @Override
            public Map<Long, String> namesOf(Collection<Long> serviceOfferingIds) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
