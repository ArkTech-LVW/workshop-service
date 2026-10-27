package br.com.arktech.workshop.inventory.infrastructure.persistence;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartFilter;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import jakarta.persistence.LockModeType;

import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

interface PartJpaRepository extends JpaRepository<Part, Long>, JpaSpecificationExecutor<Part>, PartRepository {

    Sort PAGE_ORDER = Sort.by("name", "id");
    char LIKE_ESCAPE = '\\';

    @Override
    default Page<Part> findPage(PartFilter filter, Pagination pagination) {
        var page = findAll(
                nameContaining(filter.name()).and(skuEquals(filter.sku())).and(typeEquals(filter.type())),
                PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Part> findForUpdateById(Long id);

    @Override
    default void remove(Part part) {
        try {
            delete(part);
            flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("part.inUse", part.getId().toString());
        }
    }

    private static Specification<Part> nameContaining(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + escapeLike(name.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    private static Specification<Part> skuEquals(@Nullable Sku sku) {
        if (sku == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("sku"), sku);
    }

    private static Specification<Part> typeEquals(@Nullable PartType type) {
        if (type == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
