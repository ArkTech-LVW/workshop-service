package br.com.arktech.workshop.catalog.application.serviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CatalogServiceOfferingDirectoryTest {

    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final CatalogServiceOfferingDirectory serviceOfferingDirectory =
            new CatalogServiceOfferingDirectory(serviceOfferingRepository);

    @Test
    void shouldGiveTheLaborPriceOfAnActiveService() {
        ServiceOffering brakes = serviceOfferingRepository.save(new ServiceOffering("Freios", null, LABOR_PRICE, 60));

        assertThat(serviceOfferingDirectory.laborPriceOf(brakes.getId())).isEqualTo(LABOR_PRICE);
    }

    @Test
    void shouldRefuseAnInactiveService() {
        ServiceOffering brakes = serviceOfferingRepository.save(new ServiceOffering("Freios", null, LABOR_PRICE, 60));
        brakes.deactivate();

        assertThatThrownBy(() -> serviceOfferingDirectory.laborPriceOf(brakes.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.inactive");
    }

    @Test
    void shouldReportAnUnknownService() {
        assertThatThrownBy(() -> serviceOfferingDirectory.laborPriceOf(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }
}
