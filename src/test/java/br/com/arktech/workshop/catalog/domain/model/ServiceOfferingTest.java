package br.com.arktech.workshop.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceOfferingTest {

    private static final Money PRICE = new Money(new BigDecimal("150.00"));

    @Test
    void shouldStartActiveWithGivenData() {
        ServiceOffering serviceOffering = new ServiceOffering("Troca de óleo", "Óleo e filtro", PRICE, 60);

        assertThat(serviceOffering.getName()).isEqualTo("Troca de óleo");
        assertThat(serviceOffering.getDescription()).isEqualTo("Óleo e filtro");
        assertThat(serviceOffering.getLaborPrice()).isEqualTo(PRICE);
        assertThat(serviceOffering.getEstimatedMinutes()).isEqualTo(60);
        assertThat(serviceOffering.isActive()).isTrue();
    }

    @Test
    void shouldAcceptMissingDescription() {
        ServiceOffering serviceOffering = new ServiceOffering("Alinhamento", null, PRICE, 30);

        assertThat(serviceOffering.getDescription()).isNull();
    }

    @Test
    void shouldRejectZeroLaborPrice() {
        assertThatThrownBy(() -> new ServiceOffering("Troca de óleo", null, Money.ZERO, 60))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.laborPriceNotPositive");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveEstimatedMinutes(int estimatedMinutes) {
        assertThatThrownBy(() -> new ServiceOffering("Troca de óleo", null, PRICE, estimatedMinutes))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.estimatedMinutesNotPositive");
    }

    @Test
    void shouldReplaceData() {
        ServiceOffering serviceOffering = new ServiceOffering("Troca de óleo", "Óleo e filtro", PRICE, 60);

        serviceOffering.rename("Troca de óleo sintético");
        serviceOffering.describe(null);
        serviceOffering.changeLaborPrice(new Money(new BigDecimal("180.00")));
        serviceOffering.changeEstimatedMinutes(75);

        assertThat(serviceOffering.getName()).isEqualTo("Troca de óleo sintético");
        assertThat(serviceOffering.getDescription()).isNull();
        assertThat(serviceOffering.getLaborPrice()).isEqualTo(new Money(new BigDecimal("180.00")));
        assertThat(serviceOffering.getEstimatedMinutes()).isEqualTo(75);
    }

    @Test
    void shouldKeepLaborPriceAndMinutesWhenChangesAreInvalid() {
        ServiceOffering serviceOffering = new ServiceOffering("Troca de óleo", null, PRICE, 60);

        assertThatThrownBy(() -> serviceOffering.changeLaborPrice(Money.ZERO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.laborPriceNotPositive");
        assertThatThrownBy(() -> serviceOffering.changeEstimatedMinutes(0))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOffering.estimatedMinutesNotPositive");
        assertThat(serviceOffering.getLaborPrice()).isEqualTo(PRICE);
        assertThat(serviceOffering.getEstimatedMinutes()).isEqualTo(60);
    }

    @Test
    void shouldDeactivateAndActivate() {
        ServiceOffering serviceOffering = new ServiceOffering("Troca de óleo", null, PRICE, 60);

        serviceOffering.deactivate();
        assertThat(serviceOffering.isActive()).isFalse();

        serviceOffering.activate();
        assertThat(serviceOffering.isActive()).isTrue();
    }
}
