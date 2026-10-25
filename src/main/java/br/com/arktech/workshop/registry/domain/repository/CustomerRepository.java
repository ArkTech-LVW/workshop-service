package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.registry.domain.model.Customer;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    boolean existsByDocument(CPFCNPJ document);

    boolean existsByDocumentAndIdNot(CPFCNPJ document, Long id);

    Page<Customer> findPage(CustomerFilter filter, Pagination pagination);

    default Customer findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("customer.notFound", id.toString()));
    }
}
