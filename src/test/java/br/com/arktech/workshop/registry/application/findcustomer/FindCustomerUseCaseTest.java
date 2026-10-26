package br.com.arktech.workshop.registry.application.findcustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import org.junit.jupiter.api.Test;

class FindCustomerUseCaseTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final FindCustomerUseCase findCustomerUseCase = new FindCustomerUseCase(customerRepository);

    @Test
    void shouldFindCustomer() {
        Customer customer = customerRepository.save(
                new Customer("Ana Souza", CPFCNPJ.of("52998224725"), new Email("ana@email.com"), null));

        CustomerResult result = findCustomerUseCase.execute(new FindCustomerQuery(customer.getId()));

        assertThat(result)
                .isEqualTo(new CustomerResult(
                        customer.getId(), "Ana Souza", "52998224725", PersonType.NATURAL_PERSON, "ana@email.com", null));
    }

    @Test
    void shouldReportUnknownCustomer() {
        assertThatThrownBy(() -> findCustomerUseCase.execute(new FindCustomerQuery(1234L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound")
                .extracting(error -> ((NotFoundException) error).getArgs())
                .isEqualTo(new Object[] {"1234"});
    }
}
