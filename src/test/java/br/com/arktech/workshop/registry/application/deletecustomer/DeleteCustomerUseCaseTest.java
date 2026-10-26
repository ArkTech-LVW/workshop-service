package br.com.arktech.workshop.registry.application.deletecustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import org.junit.jupiter.api.Test;

class DeleteCustomerUseCaseTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final DeleteCustomerUseCase deleteCustomerUseCase = new DeleteCustomerUseCase(customerRepository);

    @Test
    void shouldRemoveCustomer() {
        Customer ana = save("Ana", "52998224725");
        Customer bia = save("Bia", "12345678909");

        deleteCustomerUseCase.execute(new DeleteCustomerCommand(ana.getId()));

        assertThat(customerRepository.findAll()).containsExactly(bia);
    }

    @Test
    void shouldReportUnknownCustomer() {
        assertThatThrownBy(() -> deleteCustomerUseCase.execute(new DeleteCustomerCommand(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
    }

    private Customer save(String name, String document) {
        return customerRepository.save(new Customer(name, CPFCNPJ.of(document), new Email("cliente@email.com"), null));
    }
}
