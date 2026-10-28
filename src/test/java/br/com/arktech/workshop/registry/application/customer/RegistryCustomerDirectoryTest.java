package br.com.arktech.workshop.registry.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;

import org.junit.jupiter.api.Test;

class RegistryCustomerDirectoryTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final RegistryCustomerDirectory customerDirectory = new RegistryCustomerDirectory(customerRepository);

    @Test
    void shouldFindTheIdOfTheCustomerWithTheDocument() {
        Customer ana = customerRepository.save(
                new Customer("Ana", CPFCNPJ.of("52998224725"), new Email("ana@email.com"), null));

        assertThat(customerDirectory.findIdByDocument(CPFCNPJ.of("529.982.247-25"))).contains(ana.getId());
    }

    @Test
    void shouldFindNothingForAnUnknownDocument() {
        assertThat(customerDirectory.findIdByDocument(CPFCNPJ.of("52998224725"))).isEmpty();
    }
}
