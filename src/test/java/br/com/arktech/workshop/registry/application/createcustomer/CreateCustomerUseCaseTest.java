package br.com.arktech.workshop.registry.application.createcustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.InMemoryCustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import org.junit.jupiter.api.Test;

class CreateCustomerUseCaseTest {

    private final InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
    private final CreateCustomerUseCase createCustomerUseCase = new CreateCustomerUseCase(customerRepository);

    @Test
    void shouldCreateCustomerWithNormalizedDocumentAndContact() {
        CustomerResult result = createCustomerUseCase.execute(
                new CreateCustomerCommand("Ana Souza", "529.982.247-25", " Ana@Email.com", "(11) 98765-4321"));

        assertThat(result)
                .isEqualTo(new CustomerResult(
                        1L, "Ana Souza", "52998224725", PersonType.NATURAL_PERSON, "ana@email.com", "11987654321"));
        Customer customer = customerRepository.findRequiredById(result.id());
        assertThat(customer.getDocument()).isEqualTo(CPFCNPJ.of("52998224725"));
        assertThat(customer.getEmail()).isEqualTo(new Email("ana@email.com"));
        assertThat(customer.getPhone()).isEqualTo(new Phone("11987654321"));
    }

    @Test
    void shouldCreateCompanyWithoutPhone() {
        CustomerResult result = createCustomerUseCase.execute(
                new CreateCustomerCommand("Oficina Ltda", "11222333000181", "contato@oficina.com", null));

        assertThat(result.personType()).isEqualTo(PersonType.LEGAL_PERSON);
        assertThat(result.phone()).isNull();
    }

    @Test
    void shouldRejectDocumentAlreadyRegisteredWithOrWithoutMask() {
        createCustomerUseCase.execute(new CreateCustomerCommand("Ana Souza", "52998224725", "ana@email.com", null));

        assertThatThrownBy(() -> createCustomerUseCase.execute(
                        new CreateCustomerCommand("Outra Ana", "529.982.247-25", "outra@email.com", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("customer.documentAlreadyExists")
                .extracting(error -> ((ConflictException) error).getArgs())
                .isEqualTo(new Object[] {"529.982.247-25"});
        assertThat(customerRepository.findAll()).hasSize(1);
    }
}
