package br.com.arktech.workshop.registry.application.createcustomer;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCustomerUseCase implements UseCase<CreateCustomerCommand, CustomerResult> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResult execute(CreateCustomerCommand command) {
        CPFCNPJ document = CPFCNPJ.of(command.document());
        if (customerRepository.existsByDocument(document)) {
            throw new ConflictException("customer.documentAlreadyExists", document.formatted());
        }

        Phone phone = command.phone() == null ? null : new Phone(command.phone());
        Customer customer = new Customer(command.name(), document, new Email(command.email()), phone);
        return CustomerResult.from(customerRepository.save(customer));
    }
}
