package br.com.arktech.workshop.catalog.application.listserviceofferings;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ListServiceOfferingsUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final ListServiceOfferingsUseCase listServiceOfferingsUseCase =
            new ListServiceOfferingsUseCase(serviceOfferingRepository);

    @Test
    void shouldListRequestedPageOrderedByName() {
        save("Troca de óleo");
        save("Alinhamento");
        save("Balanceamento");

        Page<ServiceOfferingResult> page =
                listServiceOfferingsUseCase.execute(new ListServiceOfferingsQuery(null, 1, 2));

        assertThat(page.content()).extracting(ServiceOfferingResult::name).containsExactly("Troca de óleo");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldFilterByNameIgnoringCase() {
        save("Troca de óleo");
        save("Troca de pastilhas");
        save("Alinhamento");

        Page<ServiceOfferingResult> page =
                listServiceOfferingsUseCase.execute(new ListServiceOfferingsQuery("TROCA", 0, 20));

        assertThat(page.content())
                .extracting(ServiceOfferingResult::name)
                .containsExactly("Troca de pastilhas", "Troca de óleo");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyPageWhenNothingMatches() {
        save("Alinhamento");

        Page<ServiceOfferingResult> page =
                listServiceOfferingsUseCase.execute(new ListServiceOfferingsQuery("freio", 0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    private void save(String name) {
        serviceOfferingRepository.save(new ServiceOffering(name, null, new Money(new BigDecimal("100")), 30));
    }
}
