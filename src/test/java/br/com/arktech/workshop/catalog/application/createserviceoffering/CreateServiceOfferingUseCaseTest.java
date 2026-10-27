package br.com.arktech.workshop.catalog.application.createserviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CreateServiceOfferingUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final CreateServiceOfferingUseCase createServiceOfferingUseCase =
            new CreateServiceOfferingUseCase(serviceOfferingRepository);

    @Test
    void shouldCreateActiveServiceOffering() {
        ServiceOfferingResult result = createServiceOfferingUseCase.execute(
                new CreateServiceOfferingCommand("Troca de óleo", "Óleo e filtro", new BigDecimal("150.5"), 60));

        assertThat(result)
                .isEqualTo(new ServiceOfferingResult(
                        1L, "Troca de óleo", "Óleo e filtro", new BigDecimal("150.50"), 60, true));
        ServiceOffering serviceOffering = serviceOfferingRepository.findRequiredById(result.id());
        assertThat(serviceOffering.getLaborPrice()).isEqualTo(new Money(new BigDecimal("150.50")));
    }

    @Test
    void shouldCreateServiceOfferingWithoutDescription() {
        ServiceOfferingResult result = createServiceOfferingUseCase.execute(
                new CreateServiceOfferingCommand("Alinhamento", null, new BigDecimal("80"), 30));

        assertThat(result.description()).isNull();
    }

    @Test
    void shouldRejectZeroLaborPrice() {
        assertThatThrownBy(() -> createServiceOfferingUseCase.execute(
                        new CreateServiceOfferingCommand("Alinhamento", null, BigDecimal.ZERO, 30)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.laborPriceNotPositive");
    }

    @Test
    void shouldRejectLaborPriceWithMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> createServiceOfferingUseCase.execute(
                        new CreateServiceOfferingCommand("Alinhamento", null, new BigDecimal("80.005"), 30)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("money.tooManyDecimals");
    }
}
