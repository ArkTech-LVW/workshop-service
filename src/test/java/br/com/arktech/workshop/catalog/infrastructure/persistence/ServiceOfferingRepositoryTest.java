package br.com.arktech.workshop.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingFilter;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ServiceOfferingRepositoryTest {

    private static final Pagination FIRST_PAGE = new Pagination(0, 20);

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistDataAndTimestamps() {
        ServiceOffering saved = serviceOfferingRepository.save(
                new ServiceOffering("Troca de óleo", "Óleo e filtro", new Money(new BigDecimal("150.5")), 60));
        flushAndClear();

        ServiceOffering found = serviceOfferingRepository.findRequiredById(saved.getId());

        assertThat(found.getName()).isEqualTo("Troca de óleo");
        assertThat(found.getDescription()).isEqualTo("Óleo e filtro");
        assertThat(found.getLaborPrice()).isEqualTo(new Money(new BigDecimal("150.50")));
        assertThat(found.getEstimatedMinutes()).isEqualTo(60);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getLastUpdated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(columnsOf(saved))
                .containsEntry("labor_price", new BigDecimal("150.50"))
                .containsEntry("active", true);
    }

    @Test
    void shouldPersistServiceOfferingWithoutDescription() {
        ServiceOffering saved = serviceOfferingRepository.save(newServiceOffering("Alinhamento"));
        flushAndClear();

        assertThat(serviceOfferingRepository.findRequiredById(saved.getId()).getDescription()).isNull();
    }

    @Test
    void shouldPageOrderedByName() {
        String prefix = uniquePrefix();
        serviceOfferingRepository.save(newServiceOffering(prefix + "c"));
        serviceOfferingRepository.save(newServiceOffering(prefix + "a"));
        serviceOfferingRepository.save(newServiceOffering(prefix + "b"));
        flushAndClear();

        Page<ServiceOffering> second =
                serviceOfferingRepository.findPage(new ServiceOfferingFilter(prefix), new Pagination(1, 2));

        assertThat(second.totalElements()).isEqualTo(3);
        assertThat(second.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(ServiceOffering::getName).containsExactly(prefix + "c");
    }

    @Test
    void shouldFilterByNameIgnoringCase() {
        String prefix = uniquePrefix();
        serviceOfferingRepository.save(newServiceOffering(prefix + " Troca de Óleo"));
        serviceOfferingRepository.save(newServiceOffering(prefix + " Troca de pastilhas"));
        serviceOfferingRepository.save(newServiceOffering(prefix + " Óleo de câmbio"));
        flushAndClear();

        Page<ServiceOffering> byName = serviceOfferingRepository.findPage(
                new ServiceOfferingFilter(prefix.toUpperCase() + " TROCA"), FIRST_PAGE);
        Page<ServiceOffering> byAccentedName =
                serviceOfferingRepository.findPage(new ServiceOfferingFilter(prefix + " óleo"), FIRST_PAGE);

        assertThat(byName.content())
                .extracting(ServiceOffering::getName)
                .containsExactlyInAnyOrder(prefix + " Troca de Óleo", prefix + " Troca de pastilhas");
        assertThat(byAccentedName.content())
                .extracting(ServiceOffering::getName)
                .containsExactly(prefix + " Óleo de câmbio");
    }

    @Test
    void shouldTreatLikeWildcardsInNameFilterAsText() {
        String prefix = uniquePrefix();
        serviceOfferingRepository.save(newServiceOffering(prefix + " 100% sintético"));
        serviceOfferingRepository.save(newServiceOffering(prefix + " 100 sintético"));
        flushAndClear();

        Page<ServiceOffering> found =
                serviceOfferingRepository.findPage(new ServiceOfferingFilter(prefix + " 100%"), FIRST_PAGE);

        assertThat(found.content()).extracting(ServiceOffering::getName).containsExactly(prefix + " 100% sintético");
    }

    @Test
    void shouldRejectNonPositiveLaborPriceInDatabase() {
        ServiceOffering saved = serviceOfferingRepository.save(newServiceOffering("Troca de óleo"));
        flushAndClear();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE service_offering SET labor_price = 0 WHERE id = ?", saved.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReportUnknownServiceOffering() {
        assertThatThrownBy(() -> serviceOfferingRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOffering.notFound");
    }

    private static ServiceOffering newServiceOffering(String name) {
        return new ServiceOffering(name, null, new Money(new BigDecimal("100")), 30);
    }

    private static String uniquePrefix() {
        return "zzz-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> columnsOf(ServiceOffering serviceOffering) {
        return jdbcTemplate.queryForMap(
                "SELECT labor_price, active FROM service_offering WHERE id = ?", serviceOffering.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
