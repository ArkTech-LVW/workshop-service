package br.com.arktech.workshop.registry.application.findcustomer;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindCustomerUseCase implements UseCase<FindCustomerQuery, CustomerResult> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerResult execute(FindCustomerQuery query) {
        return CustomerResult.from(customerRepository.findRequiredById(query.id()));
    }
}
