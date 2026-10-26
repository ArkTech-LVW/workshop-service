package br.com.arktech.workshop.registry.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.repository.CustomerFilter;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import jakarta.persistence.EntityManager;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CustomerRepositoryTest {

    private static final CustomerFilter NO_FILTER = new CustomerFilter(null);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistDocumentPersonTypeContactAndTimestamps() {
        Customer saved = customerRepository.save(new Customer(
                "Oficina Ltda",
                CPFCNPJ.of("12.ABC.345/01DE-35"),
                new Email("contato@oficina.com"),
                new Phone("1134567890")));
        flushAndClear();

        Customer found = customerRepository.findRequiredById(saved.getId());

        assertThat(found.getName()).isEqualTo("Oficina Ltda");
        assertThat(found.getDocument()).isEqualTo(CPFCNPJ.of("12ABC34501DE35"));
        assertThat(found.getPersonType()).isEqualTo(PersonType.LEGAL_PERSON);
        assertThat(found.getEmail()).isEqualTo(new Email("contato@oficina.com"));
        assertThat(found.getPhone()).isEqualTo(new Phone("1134567890"));
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getLastUpdated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(columnsOf(saved))
                .containsEntry("document", "12ABC34501DE35")
                .containsEntry("person_type", "LEGAL_PERSON")
                .containsEntry("phone", "1134567890");
    }

    @Test
    void shouldPersistCustomerWithoutPhone() {
        Customer saved = customerRepository.save(newCustomer("Ana", "529.982.247-25"));
        flushAndClear();

        assertThat(customerRepository.findRequiredById(saved.getId()).getPhone()).isNull();
    }

    @Test
    void shouldCheckWhetherDocumentIsRegistered() {
        customerRepository.save(newCustomer("Ana", "529.982.247-25"));
        flushAndClear();

        assertThat(customerRepository.existsByDocument(CPFCNPJ.of("52998224725"))).isTrue();
        assertThat(customerRepository.existsByDocument(CPFCNPJ.of("12345678909"))).isFalse();
    }

    @Test
    void shouldPageCustomersOrderedByName() {
        customerRepository.save(newCustomer("zzz-c", "529.982.247-25"));
        customerRepository.save(newCustomer("zzz-a", "123.456.789-09"));
        customerRepository.save(newCustomer("zzz-b", "11.222.333/0001-81"));
        flushAndClear();
        int total = (int) customerRepository.findPage(NO_FILTER, new Pagination(0, 1)).totalElements();

        Page<Customer> last = customerRepository.findPage(NO_FILTER, new Pagination(total - 1, 1));

        assertThat(last.totalPages()).isEqualTo(total);
        assertThat(nameAt(total - 3)).isEqualTo("zzz-a");
        assertThat(nameAt(total - 2)).isEqualTo("zzz-b");
        assertThat(last.content()).extracting(Customer::getName).containsExactly("zzz-c");
    }

    @Test
    void shouldFilterByDocument() {
        customerRepository.save(newCustomer("Ana", "529.982.247-25"));
        customerRepository.save(newCustomer("Bia", "123.456.789-09"));
        flushAndClear();

        Page<Customer> found =
                customerRepository.findPage(new CustomerFilter(CPFCNPJ.of("12345678909")), new Pagination(0, 20));
        Page<Customer> missing =
                customerRepository.findPage(new CustomerFilter(CPFCNPJ.of("11222333000181")), new Pagination(0, 20));

        assertThat(found.content()).extracting(Customer::getName).containsExactly("Bia");
        assertThat(found.totalElements()).isEqualTo(1);
        assertThat(missing.content()).isEmpty();
        assertThat(missing.totalElements()).isZero();
    }

    @Test
    void shouldRejectDuplicatedDocument() {
        customerRepository.save(newCustomer("Ana", "529.982.247-25"));
        flushAndClear();

        assertThatThrownBy(() -> {
                    customerRepository.save(newCustomer("Outra Ana", "52998224725"));
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReportUnknownCustomer() {
        assertThatThrownBy(() -> customerRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("customer.notFound");
    }

    private static Customer newCustomer(String name, String document) {
        return new Customer(name, CPFCNPJ.of(document), new Email("cliente@email.com"), null);
    }

    private Map<String, Object> columnsOf(Customer customer) {
        return jdbcTemplate.queryForMap(
                "SELECT document, person_type, phone FROM customer WHERE id = ?", customer.getId());
    }

    private String nameAt(int index) {
        return customerRepository.findPage(NO_FILTER, new Pagination(index, 1)).content().getFirst().getName();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
