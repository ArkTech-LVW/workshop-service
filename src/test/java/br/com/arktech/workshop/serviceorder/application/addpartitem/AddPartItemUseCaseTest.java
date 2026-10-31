package br.com.arktech.workshop.serviceorder.application.addpartitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.application.inventory.PartDirectory;
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

class AddPartItemUseCaseTest {

    private static final Long PAD_ID = 40L;
    private static final Money SALE_PRICE = new Money(new BigDecimal("35.90"));

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();

    private final ServiceOrder serviceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), 10L, 20L, "Barulho ao frear", 1L));

    private final List<StockCheck> stockChecks = new ArrayList<>();

    @Test
    void shouldAddThePartWithItsSalePrice() {
        AddPartItemResult result = useCaseWithStock(10).execute(new AddPartItemCommand(serviceOrder.getId(), PAD_ID, 3));

        assertThat(result.id()).isNotNull();
        assertThat(result.partId()).isEqualTo(PAD_ID);
        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.unitPrice()).isEqualByComparingTo("35.90");
        assertThat(result.subtotal()).isEqualByComparingTo("107.70");
        assertThat(result.serviceOrderTotalPrice()).isEqualByComparingTo("107.70");
        assertThat(result.availableQuantity()).isEqualTo(10);
        assertThat(stockChecks).containsExactly(new StockCheck(serviceOrder.getId(), PAD_ID, 3));
        assertThat(serviceOrder.getPartItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(result.id());
            assertThat(item.getUnitPrice()).isEqualTo(SALE_PRICE);
        });
    }

    @Test
    void shouldRefuseThePartWhenTheAvailableStockDoesNotCoverIt() {
        AddPartItemUseCase addPartItemUseCase = useCaseWithStock(2);

        assertThatThrownBy(() -> addPartItemUseCase.execute(new AddPartItemCommand(serviceOrder.getId(), PAD_ID, 3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
    }

    @Test
    void shouldCheckEveryItemOfTheSamePartAgainstTheAvailableStock() {
        AddPartItemUseCase addPartItemUseCase = useCaseWithStock(4);
        addPartItemUseCase.execute(new AddPartItemCommand(serviceOrder.getId(), PAD_ID, 2));
        AddPartItemResult other = addPartItemUseCase.execute(new AddPartItemCommand(serviceOrder.getId(), PAD_ID + 1, 4));

        assertThatThrownBy(() -> addPartItemUseCase.execute(new AddPartItemCommand(serviceOrder.getId(), PAD_ID, 3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("part.insufficientStock");
        assertThat(stockChecks)
                .extracting(StockCheck::quantity)
                .containsExactly(2, 4, 5);
        assertThat(other.serviceOrderTotalPrice()).isEqualByComparingTo("215.40");
    }

    @Test
    void shouldReportAnUnknownPart() {
        AddPartItemUseCase withUnknownPart = new AddPartItemUseCase(serviceOrderRepository, stocking(partId -> {
            throw new NotFoundException("part.notFound", partId.toString());
        }, 10));

        assertThatThrownBy(() -> withUnknownPart.execute(new AddPartItemCommand(serviceOrder.getId(), 99L, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
        assertThat(serviceOrder.getPartItems()).isEmpty();
    }

    @Test
    void shouldReportAnUnknownServiceOrder() {
        assertThatThrownBy(() -> useCaseWithStock(10).execute(new AddPartItemCommand(99L, PAD_ID, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }

    private AddPartItemUseCase useCaseWithStock(int availableQuantity) {
        return new AddPartItemUseCase(
                serviceOrderRepository,
                stocking(partId -> new PartDetails(SALE_PRICE, availableQuantity), availableQuantity));
    }

    private PartDirectory stocking(Function<Long, PartDetails> detailsOf, int availableQuantity) {
        return new PartDirectory() {
            @Override
            public PartDetails detailsOf(Long partId) {
                return detailsOf.apply(partId);
            }

            @Override
            public Map<Long, String> namesOf(Collection<Long> partIds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void requireAvailableFor(Long serviceOrderId, Long partId, int quantity) {
                stockChecks.add(new StockCheck(serviceOrderId, partId, quantity));
                if (quantity > availableQuantity) {
                    throw new BusinessRuleException("part.insufficientStock");
                }
            }
        };
    }

    private record StockCheck(Long serviceOrderId, Long partId, int quantity) {
    }
}
