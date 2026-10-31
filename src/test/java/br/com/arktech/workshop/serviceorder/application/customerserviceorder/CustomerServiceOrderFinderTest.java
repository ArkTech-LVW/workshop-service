package br.com.arktech.workshop.serviceorder.application.customerserviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.InMemoryServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerServiceOrderFinderTest {

    private static final Long ANA_ID = 10L;
    private static final Long BIA_ID = 11L;
    private static final String ANA_DOCUMENT = "529.982.247-25";
    private static final String BIA_DOCUMENT = "123.456.789-09";

    private final InMemoryServiceOrderRepository serviceOrderRepository = new InMemoryServiceOrderRepository();
    private final Map<CPFCNPJ, Long> customers =
            Map.of(CPFCNPJ.of(ANA_DOCUMENT), ANA_ID, CPFCNPJ.of(BIA_DOCUMENT), BIA_ID);
    private final CustomerDirectory customerDirectory = document -> Optional.ofNullable(customers.get(document));
    private final CustomerServiceOrderFinder finder =
            new CustomerServiceOrderFinder(serviceOrderRepository, customerDirectory);

    private final ServiceOrder anasServiceOrder = serviceOrderRepository.save(
            new ServiceOrder(serviceOrderRepository.nextNumber(), ANA_ID, 20L, "Barulho ao frear", 1L));

    @ParameterizedTest
    @ValueSource(strings = {ANA_DOCUMENT, "52998224725"})
    void shouldFindTheServiceOrderOfTheCustomerWhoOwnsIt(String document) {
        assertThat(finder.findRequired(anasServiceOrder.getPublicId(), document)).isSameAs(anasServiceOrder);
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownPublicId() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> finder.findRequired(unknown, ANA_DOCUMENT))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("serviceOrder.notFound");
                    assertThat(ex.getArgs()).containsExactly(unknown.toString());
                });
    }

    @Test
    void shouldAnswerNotFoundWhenTheDocumentIsFromAnotherCustomer() {
        assertThatThrownBy(() -> finder.findRequired(anasServiceOrder.getPublicId(), BIA_DOCUMENT))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("serviceOrder.notFound");
                    assertThat(ex.getArgs()).containsExactly(anasServiceOrder.getPublicId().toString());
                });
    }

    @Test
    void shouldAnswerNotFoundWhenTheDocumentIsNotRegistered() {
        assertThatThrownBy(() -> finder.findRequired(anasServiceOrder.getPublicId(), "111.444.777-35"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.notFound");
    }

    @Test
    void shouldRejectAMalformedDocument() {
        assertThatThrownBy(() -> finder.findRequired(anasServiceOrder.getPublicId(), "123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("cpfCnpj.invalid");
    }
}
