package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final List<Customer> customers = new ArrayList<>();

    private long nextId = 1;

    @Override
    public Customer save(Customer customer) {
        if (!customers.contains(customer)) {
            ReflectionTestUtils.setField(customer, "id", nextId++);
            customers.add(customer);
        }
        return customer;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customers.stream().filter(customer -> customer.getId().equals(id)).findFirst();
    }

    @Override
    public boolean existsByDocument(CPFCNPJ document) {
        return customers.stream().anyMatch(customer -> customer.getDocument().equals(document));
    }

    @Override
    public boolean existsByDocumentAndIdNot(CPFCNPJ document, Long id) {
        return customers.stream()
                .anyMatch(customer -> customer.getDocument().equals(document) && !customer.getId().equals(id));
    }

    @Override
    public Page<Customer> findPage(CustomerFilter filter, Pagination pagination) {
        List<Customer> matching = customers.stream()
                .filter(customer -> filter.document() == null || customer.getDocument().equals(filter.document()))
                .sorted(Comparator.comparing(Customer::getName).thenComparing(Customer::getId))
                .toList();
        List<Customer> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    @Override
    public void remove(Customer customer) {
        customers.remove(customer);
    }

    public List<Customer> findAll() {
        return List.copyOf(customers);
    }
}
