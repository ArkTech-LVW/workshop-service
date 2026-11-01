package br.com.arktech.workshop.registry.application.customer;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerContacts;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RegistryCustomerDirectory implements CustomerDirectory, CustomerContacts {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<Long> findIdByDocument(CPFCNPJ document) {
        return customerRepository.findByDocument(document).map(Customer::getId);
    }

    @Override
    public Optional<Email> findEmailById(Long customerId) {
        return customerRepository.findById(customerId).map(Customer::getEmail);
    }
}
