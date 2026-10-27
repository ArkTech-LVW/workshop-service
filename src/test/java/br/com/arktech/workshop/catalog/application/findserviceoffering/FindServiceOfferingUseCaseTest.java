package br.com.arktech.workshop.catalog.application.findserviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FindServiceOfferingUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final FindServiceOfferingUseCase findServiceOfferingUseCase =
            new FindServiceOfferingUseCase(serviceOfferingRepository);

    @Test
    void shouldFindServiceOffering() {
        ServiceOffering serviceOffering = serviceOfferingRepository.save(
                new ServiceOffering("Troca de óleo", null, new Money(new BigDecimal("150")), 60));

        ServiceOfferingResult result =
                findServiceOfferingUseCase.execute(new FindServiceOfferingQuery(serviceOffering.getId()));

        assertThat(result)
                .isEqualTo(new ServiceOfferingResult(
                        serviceOffering.getId(), "Troca de óleo", null, new BigDecimal("150.00"), 60, true));
    }

    @Test
    void shouldReportUnknownServiceOffering() {
        assertThatThrownBy(() -> findServiceOfferingUseCase.execute(new FindServiceOfferingQuery(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }
}
