package br.com.arktech.workshop.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SkuTest {

    @ParameterizedTest
    @CsvSource({
        "OLEO-5W30-1L,       OLEO-5W30-1L",
        "oleo-5w30-1l,       OLEO-5W30-1L",
        "' filtro_ar.01 ',   FILTRO_AR.01",
        "PAST/DIANT,         PAST/DIANT",
        "123,                123"
    })
    void shouldNormalizeToUpperCaseWithoutSurroundingSpaces(String input, String value) {
        assertThat(new Sku(input).value()).isEqualTo(value);
    }

    @Test
    void shouldBeEqualRegardlessOfCase() {
        assertThat(new Sku("oleo-5w30")).isEqualTo(new Sku("OLEO-5W30"));
    }

    @Test
    void shouldAcceptFiftyCharacters() {
        assertThat(new Sku("A".repeat(50)).value()).hasSize(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "OLEO 5W30", "ÓLEO", "OLEO#1", "OLEO+1"})
    void shouldRejectInvalidSku(String input) {
        assertThatThrownBy(() -> new Sku(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("sku.invalid");
    }

    @Test
    void shouldRejectMoreThanFiftyCharacters() {
        assertThatThrownBy(() -> new Sku("A".repeat(51)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("sku.invalid");
    }
}
