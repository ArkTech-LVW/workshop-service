package br.com.arktech.workshop.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartFilter;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
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
class PartRepositoryTest {

    private static final Pagination FIRST_PAGE = new Pagination(0, 20);

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistDataAndTimestamps() {
        String prefix = uniquePrefix();
        Part saved = partRepository.save(new Part(
                new Sku(prefix + "-oleo"),
                "Óleo 5W30 1L",
                PartType.SUPPLY,
                MeasurementUnit.LITER,
                new Money(new BigDecimal("45.9")),
                10));
        flushAndClear();

        Part found = partRepository.findRequiredById(saved.getId());

        assertThat(found.getSku()).isEqualTo(new Sku(prefix + "-OLEO"));
        assertThat(found.getName()).isEqualTo("Óleo 5W30 1L");
        assertThat(found.getType()).isEqualTo(PartType.SUPPLY);
        assertThat(found.getUnit()).isEqualTo(MeasurementUnit.LITER);
        assertThat(found.getSalePrice()).isEqualTo(new Money(new BigDecimal("45.90")));
        assertThat(found.getStockQuantity()).isZero();
        assertThat(found.getMinimumStock()).isEqualTo(10);
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getLastUpdated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(columnsOf(saved))
                .containsEntry("sku", prefix.toUpperCase() + "-OLEO")
                .containsEntry("type", "SUPPLY")
                .containsEntry("unit", "LITER")
                .containsEntry("sale_price", new BigDecimal("45.90"))
                .containsEntry("stock_quantity", 0);
    }

    @Test
    void shouldCheckSkuExistence() {
        String prefix = uniquePrefix();
        partRepository.save(newPart(prefix + "-a", "Filtro", PartType.PART));
        flushAndClear();

        assertThat(partRepository.existsBySku(new Sku(prefix + "-A"))).isTrue();
        assertThat(partRepository.existsBySku(new Sku(prefix + "-B"))).isFalse();
    }

    @Test
    void shouldRejectDuplicatedSkuInDatabase() {
        String prefix = uniquePrefix();
        partRepository.save(newPart(prefix + "-a", "Filtro", PartType.PART));
        flushAndClear();

        assertThatThrownBy(() -> {
                    partRepository.save(newPart(prefix + "-A", "Outro filtro", PartType.PART));
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPageOrderedByName() {
        String prefix = uniquePrefix();
        partRepository.save(newPart(prefix + "-1", prefix + " c", PartType.PART));
        partRepository.save(newPart(prefix + "-2", prefix + " a", PartType.PART));
        partRepository.save(newPart(prefix + "-3", prefix + " b", PartType.PART));
        flushAndClear();

        Page<Part> second = partRepository.findPage(new PartFilter(prefix, null, null), new Pagination(1, 2));

        assertThat(second.totalElements()).isEqualTo(3);
        assertThat(second.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(Part::getName).containsExactly(prefix + " c");
    }

    @Test
    void shouldFilterByNameSkuAndType() {
        String prefix = uniquePrefix();
        partRepository.save(newPart(prefix + "-1", prefix + " Filtro de Óleo", PartType.PART));
        partRepository.save(newPart(prefix + "-2", prefix + " Óleo 5W30", PartType.SUPPLY));
        partRepository.save(newPart(prefix + "-3", prefix + " Pastilha", PartType.PART));
        flushAndClear();

        Page<Part> byName =
                partRepository.findPage(new PartFilter(prefix.toUpperCase() + " filtro", null, null), FIRST_PAGE);
        Page<Part> byAccentedName = partRepository.findPage(new PartFilter(prefix + " óleo", null, null), FIRST_PAGE);
        Page<Part> bySku = partRepository.findPage(new PartFilter(null, new Sku(prefix + "-3"), null), FIRST_PAGE);
        Page<Part> byType = partRepository.findPage(new PartFilter(prefix, null, PartType.PART), FIRST_PAGE);

        assertThat(byName.content()).extracting(Part::getName).containsExactly(prefix + " Filtro de Óleo");
        assertThat(byAccentedName.content()).extracting(Part::getName).containsExactly(prefix + " Óleo 5W30");
        assertThat(bySku.content()).extracting(Part::getName).containsExactly(prefix + " Pastilha");
        assertThat(byType.content())
                .extracting(Part::getName)
                .containsExactly(prefix + " Filtro de Óleo", prefix + " Pastilha");
    }

    @Test
    void shouldTreatLikeWildcardsInNameFilterAsText() {
        String prefix = uniquePrefix();
        partRepository.save(newPart(prefix + "-1", prefix + " 100% sintético", PartType.SUPPLY));
        partRepository.save(newPart(prefix + "-2", prefix + " 100 sintético", PartType.SUPPLY));
        flushAndClear();

        Page<Part> found = partRepository.findPage(new PartFilter(prefix + " 100%", null, null), FIRST_PAGE);

        assertThat(found.content()).extracting(Part::getName).containsExactly(prefix + " 100% sintético");
    }

    @Test
    void shouldRejectNegativeStockQuantityInDatabase() {
        Part saved = partRepository.save(newPart(uniquePrefix(), "Filtro", PartType.PART));
        flushAndClear();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE part SET stock_quantity = -1 WHERE id = ?", saved.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReportUnknownPart() {
        assertThatThrownBy(() -> partRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("part.notFound");
    }

    private static Part newPart(String sku, String name, PartType type) {
        return new Part(new Sku(sku), name, type, MeasurementUnit.UNIT, new Money(new BigDecimal("10")), 0);
    }

    private static String uniquePrefix() {
        return "zzz-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> columnsOf(Part part) {
        return jdbcTemplate.queryForMap(
                "SELECT sku, type, unit, sale_price, stock_quantity FROM part WHERE id = ?", part.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
