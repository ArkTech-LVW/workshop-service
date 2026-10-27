package br.com.arktech.workshop.inventory.domain.repository;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryPartRepository implements PartRepository {

    private final List<Part> parts = new ArrayList<>();

    private long nextId = 1;

    @Override
    public Part save(Part part) {
        if (!parts.contains(part)) {
            ReflectionTestUtils.setField(part, "id", nextId++);
            parts.add(part);
        }
        return part;
    }

    @Override
    public Optional<Part> findById(Long id) {
        return parts.stream().filter(part -> part.getId().equals(id)).findFirst();
    }

    @Override
    public boolean existsBySku(Sku sku) {
        return parts.stream().anyMatch(part -> part.getSku().equals(sku));
    }

    @Override
    public boolean existsBySkuAndIdNot(Sku sku, Long id) {
        return parts.stream().anyMatch(part -> part.getSku().equals(sku) && !part.getId().equals(id));
    }

    @Override
    public Optional<Part> findForUpdateById(Long id) {
        return findById(id);
    }

    @Override
    public Page<Part> findPage(PartFilter filter, Pagination pagination) {
        return page(
                parts.stream()
                        .filter(part -> matchesName(part, filter.name()))
                        .filter(part -> filter.sku() == null || part.getSku().equals(filter.sku()))
                        .filter(part -> filter.type() == null || part.getType() == filter.type())
                        .toList(),
                pagination);
    }

    @Override
    public Page<Part> findLowStockPage(Pagination pagination) {
        return page(parts.stream().filter(Part::isLowStock).toList(), pagination);
    }

    private static Page<Part> page(List<Part> filtered, Pagination pagination) {
        List<Part> matching = filtered.stream()
                .sorted(Comparator.comparing(Part::getName).thenComparing(Part::getId))
                .toList();
        List<Part> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    @Override
    public void remove(Part part) {
        parts.remove(part);
    }

    public List<Part> findAll() {
        return List.copyOf(parts);
    }

    private static boolean matchesName(Part part, String name) {
        return name == null || part.getName().toLowerCase(Locale.ROOT).contains(name.strip().toLowerCase(Locale.ROOT));
    }
}
