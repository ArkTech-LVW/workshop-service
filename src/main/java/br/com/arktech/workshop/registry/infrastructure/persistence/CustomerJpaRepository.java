package br.com.arktech.workshop.registry.infrastructure.persistence;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.CustomerFilter;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface CustomerJpaRepository
        extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer>, CustomerRepository {

    Sort PAGE_ORDER = Sort.by("name", "id");

    @Override
    default Page<Customer> findPage(CustomerFilter filter, Pagination pagination) {
        var page = findAll(matching(filter), PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    @Override
    default void remove(Customer customer) {
        try {
            delete(customer);
            flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("customer.inUse", customer.getId().toString());
        }
    }

    private static Specification<Customer> matching(CustomerFilter filter) {
        CPFCNPJ document = filter.document();
        if (document == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("document"), document);
    }
}
