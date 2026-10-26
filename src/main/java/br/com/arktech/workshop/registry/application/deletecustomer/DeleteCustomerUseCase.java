package br.com.arktech.workshop.registry.application.deletecustomer;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCustomerUseCase implements VoidUseCase<DeleteCustomerCommand> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void execute(DeleteCustomerCommand command) {
        Customer customer = customerRepository.findRequiredById(command.id());

        customerRepository.remove(customer);
    }
}
