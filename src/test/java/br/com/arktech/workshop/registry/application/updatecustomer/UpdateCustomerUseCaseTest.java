package br.com.arktech.workshop.registry.application.updatecustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import org.junit.jupiter.api.Test;

class UpdateCustomerUseCaseTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final UpdateCustomerUseCase updateCustomerUseCase = new UpdateCustomerUseCase(customerRepository);

    @Test
    void shouldReplaceNameDocumentAndContact() {
        Customer customer = save("Ana", "52998224725");

        CustomerResult result = updateCustomerUseCase.execute(new UpdateCustomerCommand(
                customer.getId(), "Oficina Ltda", "11.222.333/0001-81", "Contato@Oficina.com", "(11) 3456-7890"));

        assertThat(result)
                .isEqualTo(new CustomerResult(
                        customer.getId(),
                        "Oficina Ltda",
                        "11222333000181",
                        PersonType.LEGAL_PERSON,
                        "contato@oficina.com",
                        "1134567890"));
        assertThat(customer.getEmail()).isEqualTo(new Email("contato@oficina.com"));
        assertThat(customer.getPhone()).isEqualTo(new Phone("1134567890"));
    }

    @Test
    void shouldKeepItsOwnDocumentAndClearPhone() {
        Customer customer = customerRepository.save(new Customer(
                "Ana", CPFCNPJ.of("52998224725"), new Email("ana@email.com"), new Phone("11987654321")));

        CustomerResult result = updateCustomerUseCase.execute(
                new UpdateCustomerCommand(customer.getId(), "Ana Souza", "529.982.247-25", "ana@email.com", null));

        assertThat(result.name()).isEqualTo("Ana Souza");
        assertThat(result.document()).isEqualTo("52998224725");
        assertThat(result.phone()).isNull();
    }

    @Test
    void shouldRejectDocumentOfAnotherCustomer() {
        Customer ana = save("Ana", "52998224725");
        save("Bia", "12345678909");

        assertThatThrownBy(() -> updateCustomerUseCase.execute(
                        new UpdateCustomerCommand(ana.getId(), "Ana", "123.456.789-09", "ana@email.com", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("customer.documentAlreadyExists");
        assertThat(ana.getDocument()).isEqualTo(CPFCNPJ.of("52998224725"));
    }

    @Test
    void shouldReportUnknownCustomer() {
        assertThatThrownBy(() -> updateCustomerUseCase.execute(
                        new UpdateCustomerCommand(1234L, "Ana", "52998224725", "ana@email.com", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
    }

    private Customer save(String name, String document) {
        return customerRepository.save(new Customer(name, CPFCNPJ.of(document), new Email("ana@email.com"), null));
    }
}
