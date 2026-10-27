package br.com.arktech.workshop.catalog.application.deactivateserviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DeactivateServiceOfferingUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final DeactivateServiceOfferingUseCase deactivateServiceOfferingUseCase =
            new DeactivateServiceOfferingUseCase(serviceOfferingRepository);

    @Test
    void shouldDeactivateServiceOffering() {
        ServiceOffering serviceOffering = serviceOfferingRepository.save(
                new ServiceOffering("Troca de óleo", null, new Money(new BigDecimal("150")), 60));

        ServiceOfferingResult result = deactivateServiceOfferingUseCase.execute(
                new DeactivateServiceOfferingCommand(serviceOffering.getId()));

        assertThat(result.active()).isFalse();
        assertThat(serviceOffering.isActive()).isFalse();
    }

    @Test
    void shouldReportUnknownServiceOffering() {
        assertThatThrownBy(() -> deactivateServiceOfferingUseCase.execute(new DeactivateServiceOfferingCommand(99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }
}
