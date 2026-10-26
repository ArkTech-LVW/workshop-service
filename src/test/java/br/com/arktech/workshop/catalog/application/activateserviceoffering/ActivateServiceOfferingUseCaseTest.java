package br.com.arktech.workshop.catalog.application.activateserviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ActivateServiceOfferingUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final ActivateServiceOfferingUseCase activateServiceOfferingUseCase =
            new ActivateServiceOfferingUseCase(serviceOfferingRepository);

    @Test
    void shouldActivateServiceOffering() {
        ServiceOffering serviceOffering = serviceOfferingRepository.save(
                new ServiceOffering("Troca de óleo", null, new Money(new BigDecimal("150")), 60));
        serviceOffering.deactivate();

        ServiceOfferingResult result =
                activateServiceOfferingUseCase.execute(new ActivateServiceOfferingCommand(serviceOffering.getId()));

        assertThat(result.active()).isTrue();
        assertThat(serviceOffering.isActive()).isTrue();
    }

    @Test
    void shouldReportUnknownServiceOffering() {
        assertThatThrownBy(() -> activateServiceOfferingUseCase.execute(new ActivateServiceOfferingCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }
}
