package br.com.arktech.workshop.registry.application.updatecustomer;

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
public class UpdateCustomerUseCase implements UseCase<UpdateCustomerCommand, CustomerResult> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResult execute(UpdateCustomerCommand command) {
        Customer customer = customerRepository.findRequiredById(command.id());
        CPFCNPJ document = CPFCNPJ.of(command.document());
        if (customerRepository.existsByDocumentAndIdNot(document, customer.getId())) {
            throw new ConflictException("customer.documentAlreadyExists", document.formatted());
        }

        Phone phone = command.phone() == null ? null : new Phone(command.phone());
        customer.rename(command.name());
        customer.changeDocument(document);
        customer.changeContact(new Email(command.email()), phone);
        return CustomerResult.from(customerRepository.save(customer));
    }
}
