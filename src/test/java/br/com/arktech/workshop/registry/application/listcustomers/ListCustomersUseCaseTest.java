package br.com.arktech.workshop.registry.application.listcustomers;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.pagination.Page;

import org.junit.jupiter.api.Test;

class ListCustomersUseCaseTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final ListCustomersUseCase listCustomersUseCase = new ListCustomersUseCase(customerRepository);

    @Test
    void shouldListRequestedPageOrderedByName() {
        save("Caio", "52998224725");
        save("Ana", "12345678909");
        save("Bia", "11222333000181");

        Page<CustomerResult> page = listCustomersUseCase.execute(new ListCustomersQuery(null, 1, 2));

        assertThat(page.content()).extracting(CustomerResult::name).containsExactly("Caio");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldFilterByDocumentWithOrWithoutMask() {
        save("Ana", "12345678909");
        save("Caio", "52998224725");

        Page<CustomerResult> page = listCustomersUseCase.execute(new ListCustomersQuery("529.982.247-25", 0, 20));

        assertThat(page.content()).extracting(CustomerResult::name).containsExactly("Caio");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyPageForUnknownDocument() {
        save("Ana", "12345678909");

        Page<CustomerResult> page = listCustomersUseCase.execute(new ListCustomersQuery("52998224725", 0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    private void save(String name, String document) {
        customerRepository.save(new Customer(name, CPFCNPJ.of(document), new Email("cliente@email.com"), null));
    }
}
