package br.com.arktech.workshop.catalog.domain.repository;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryServiceOfferingRepository implements ServiceOfferingRepository {

    private final List<ServiceOffering> serviceOfferings = new ArrayList<>();

    private long nextId = 1;

    @Override
    public ServiceOffering save(ServiceOffering serviceOffering) {
        if (!serviceOfferings.contains(serviceOffering)) {
            ReflectionTestUtils.setField(serviceOffering, "id", nextId++);
            serviceOfferings.add(serviceOffering);
        }
        return serviceOffering;
    }

    @Override
    public Optional<ServiceOffering> findById(Long id) {
        return serviceOfferings.stream()
                .filter(serviceOffering -> serviceOffering.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<ServiceOffering> findAllById(Iterable<Long> ids) {
        List<Long> wanted = new ArrayList<>();
        ids.forEach(wanted::add);
        return serviceOfferings.stream()
                .filter(serviceOffering -> wanted.contains(serviceOffering.getId()))
                .toList();
    }

    @Override
    public Page<ServiceOffering> findPage(ServiceOfferingFilter filter, Pagination pagination) {
        List<ServiceOffering> matching = serviceOfferings.stream()
                .filter(serviceOffering -> matchesName(serviceOffering, filter.name()))
                .filter(serviceOffering -> filter.active() == null || serviceOffering.isActive() == filter.active())
                .sorted(Comparator.comparing(ServiceOffering::getName).thenComparing(ServiceOffering::getId))
                .toList();
        List<ServiceOffering> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    private static boolean matchesName(ServiceOffering serviceOffering, String name) {
        return name == null
                || serviceOffering.getName().toLowerCase(Locale.ROOT).contains(name.strip().toLowerCase(Locale.ROOT));
    }
}
