package br.com.arktech.workshop.shared.domain.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CPFCNPJTest {

    @ParameterizedTest
    @CsvSource({
        "52998224725,        52998224725,    529.982.247-25,     NATURAL_PERSON",
        "529.982.247-25,     52998224725,    529.982.247-25,     NATURAL_PERSON",
        "11222333000181,     11222333000181, 11.222.333/0001-81, LEGAL_PERSON",
        "11.222.333/0001-81, 11222333000181, 11.222.333/0001-81, LEGAL_PERSON",
        "12.ABC.345/01DE-35, 12ABC34501DE35, 12.ABC.345/01DE-35, LEGAL_PERSON"
    })
    void shouldAcceptValidDocumentsWithOrWithoutMask(
            String input, String value, String formatted, PersonType personType) {
        CPFCNPJ document = CPFCNPJ.of(input);

        assertThat(CPFCNPJ.isValid(input)).isTrue();
        assertThat(document.value()).isEqualTo(value);
        assertThat(document.formatted()).isEqualTo(formatted);
        assertThat(document.personType()).isEqualTo(personType);
    }

    @Test
    void shouldConvertToAndFromColumn() {
        CPFCNPJConverter converter = new CPFCNPJConverter();

        assertThat(converter.convertToDatabaseColumn(CPFCNPJ.of("529.982.247-25"))).isEqualTo("52998224725");
        assertThat(converter.convertToEntityAttribute("52998224725")).isEqualTo(new CPF("52998224725"));
        assertThat(converter.convertToEntityAttribute("12ABC34501DE35")).isEqualTo(new CNPJ("12ABC34501DE35"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // wrong check digit
                "529.982.247-24",
                "11.222.333/0001-82",
                "12.ABC.345/01DE-36",
                // wrong length
                "5299822472",
                "529982247250",
                // repeated sequence: passes the check digits, but is not a real document
                "111.111.111-11",
                "00.000.000/0000-00",
                // characters outside the format
                "5299822472A",
                "529,982,247-25"
            })
    void shouldRejectInvalidDocuments(String input) {
        assertThat(CPFCNPJ.isValid(input)).isFalse();
        assertThatThrownBy(() -> CPFCNPJ.of(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("cpfCnpj.invalid");
    }
}
