package br.com.arktech.workshop.catalog.application.updateserviceoffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.InMemoryServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class UpdateServiceOfferingUseCaseTest {

    private final InMemoryServiceOfferingRepository serviceOfferingRepository = new InMemoryServiceOfferingRepository();
    private final UpdateServiceOfferingUseCase updateServiceOfferingUseCase =
            new UpdateServiceOfferingUseCase(serviceOfferingRepository);

    @Test
    void shouldReplaceData() {
        ServiceOffering serviceOffering = save();

        ServiceOfferingResult result = updateServiceOfferingUseCase.execute(new UpdateServiceOfferingCommand(
                serviceOffering.getId(), "Troca de óleo sintético", "Óleo 5W30", new BigDecimal("180"), 75));

        assertThat(result)
                .isEqualTo(new ServiceOfferingResult(
                        serviceOffering.getId(),
                        "Troca de óleo sintético",
                        "Óleo 5W30",
                        new BigDecimal("180.00"),
                        75,
                        true));
        assertThat(serviceOffering.getLaborPrice()).isEqualTo(new Money(new BigDecimal("180.00")));
    }

    @Test
    void shouldKeepActiveStatus() {
        ServiceOffering serviceOffering = save();
        serviceOffering.deactivate();

        ServiceOfferingResult result = updateServiceOfferingUseCase.execute(new UpdateServiceOfferingCommand(
                serviceOffering.getId(), "Troca de óleo", null, new BigDecimal("150"), 60));

        assertThat(result.active()).isFalse();
    }

    @Test
    void shouldRejectZeroLaborPrice() {
        ServiceOffering serviceOffering = save();

        assertThatThrownBy(() -> updateServiceOfferingUseCase.execute(new UpdateServiceOfferingCommand(
                        serviceOffering.getId(), "Troca de óleo", null, BigDecimal.ZERO, 60)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.laborPriceNotPositive");
    }

    @Test
    void shouldReportUnknownServiceOffering() {
        assertThatThrownBy(() -> updateServiceOfferingUseCase.execute(
                        new UpdateServiceOfferingCommand(1234L, "Troca de óleo", null, new BigDecimal("150"), 60)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }

    private ServiceOffering save() {
        return serviceOfferingRepository.save(
                new ServiceOffering("Troca de óleo", "Óleo e filtro", new Money(new BigDecimal("150")), 60));
    }
}
