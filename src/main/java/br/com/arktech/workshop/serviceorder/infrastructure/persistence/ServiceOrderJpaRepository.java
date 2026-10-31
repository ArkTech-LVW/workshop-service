package br.com.arktech.workshop.serviceorder.infrastructure.persistence;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderFilter;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import jakarta.persistence.criteria.Expression;

import java.time.Instant;
import java.time.Year;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface ServiceOrderJpaRepository
        extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder>, ServiceOrderRepository {

    List<ServiceOrderStatus> CLOSED_STATUSES = Arrays.stream(ServiceOrderStatus.values())
            .filter(ServiceOrderStatus::isClosed)
            .toList();

    @Override
    default ServiceOrderNumber nextNumber() {
        Year year = Year.now();
        return ServiceOrderNumber.of(year, incrementNumberCounter(year.getValue()));
    }

    @Transactional
    @Query(
            value = """
                    INSERT INTO service_order_number_counter (year, last_value) VALUES (:year, 1)
                    ON CONFLICT (year) DO UPDATE
                    SET last_value = service_order_number_counter.last_value + 1,
                        last_updated = now(),
                        version = service_order_number_counter.version + 1
                    RETURNING last_value
                    """,
            nativeQuery = true)
    long incrementNumberCounter(@Param("year") int year);

    @Override
    default Page<ServiceOrder> findPage(ServiceOrderFilter filter, Pagination pagination) {
        var page = findAll(
                statusEquals(filter.status())
                        .and(customerIdEquals(filter.customerId()))
                        .and(vehicleIdEquals(filter.vehicleId()))
                        .and(openedFrom(filter.openedFrom()))
                        .and(openedTo(filter.openedTo()))
                        .and(openFirstThenOldestFirst()),
                PageRequest.of(pagination.page(), pagination.size()));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    private static Specification<ServiceOrder> statusEquals(@Nullable ServiceOrderStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private static Specification<ServiceOrder> customerIdEquals(@Nullable Long customerId) {
        if (customerId == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("customerId"), customerId);
    }

    private static Specification<ServiceOrder> vehicleIdEquals(@Nullable Long vehicleId) {
        if (vehicleId == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.equal(root.get("vehicleId"), vehicleId);
    }

    private static Specification<ServiceOrder> openedFrom(@Nullable Instant from) {
        if (from == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("dateCreated"), from);
    }

    private static Specification<ServiceOrder> openedTo(@Nullable Instant to) {
        if (to == null) {
            return Specification.unrestricted();
        }

        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("dateCreated"), to);
    }

    private static Specification<ServiceOrder> openFirstThenOldestFirst() {
        return (root, query, builder) -> {
            if (!Long.class.equals(query.getResultType())) {
                Expression<Integer> closedLast = builder.<Integer>selectCase()
                        .when(root.get("status").in(CLOSED_STATUSES), 1)
                        .otherwise(0);
                query.orderBy(
                        builder.asc(closedLast), builder.asc(root.get("dateCreated")), builder.asc(root.get("id")));
            }
            return builder.conjunction();
        };
    }
}
