package br.com.arktech.workshop.catalog.infrastructure.persistence;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingFilter;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ServiceOfferingJpaRepository
        extends JpaRepository<ServiceOffering, Long>,
                JpaSpecificationExecutor<ServiceOffering>,
                ServiceOfferingRepository {

    Sort PAGE_ORDER = Sort.by("name", "id");
    char LIKE_ESCAPE = '\\';

    @Override
    default Page<ServiceOffering> findPage(ServiceOfferingFilter filter, Pagination pagination) {
        var page = findAll(
                nameContaining(filter.name()), PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    private static Specification<ServiceOffering> nameContaining(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + escapeLike(name.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
