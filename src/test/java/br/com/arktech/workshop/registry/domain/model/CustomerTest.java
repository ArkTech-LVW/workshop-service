package br.com.arktech.workshop.registry.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final CPFCNPJ CPF = CPFCNPJ.of("529.982.247-25");
    private static final CPFCNPJ CNPJ = CPFCNPJ.of("11.222.333/0001-81");
    private static final Email EMAIL = new Email("ana@email.com");

    @Test
    void shouldDerivePersonTypeFromDocument() {
        Customer person = new Customer("Ana Souza", CPF, EMAIL, null);
        Customer company = new Customer("Oficina Ltda", CNPJ, EMAIL, null);

        assertThat(person.getPersonType()).isEqualTo(PersonType.NATURAL_PERSON);
        assertThat(company.getPersonType()).isEqualTo(PersonType.LEGAL_PERSON);
    }

    @Test
    void shouldKeepNameDocumentAndContact() {
        Customer customer = new Customer("Ana Souza", CPF, EMAIL, new Phone("11987654321"));

        assertThat(customer.getName()).isEqualTo("Ana Souza");
        assertThat(customer.getDocument()).isEqualTo(CPF);
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getPhone()).isEqualTo(new Phone("11987654321"));
    }
}
