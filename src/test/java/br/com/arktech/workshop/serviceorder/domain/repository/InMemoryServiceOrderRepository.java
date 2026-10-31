package br.com.arktech.workshop.serviceorder.domain.repository;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryServiceOrderRepository implements ServiceOrderRepository {

    private final List<ServiceOrder> serviceOrders = new ArrayList<>();

    private final Consumer<Object> eventPublisher;

    private long nextId = 1;

    private long nextSequence = 1;

    private long nextItemId = 1;

    public InMemoryServiceOrderRepository() {
        this(event -> {});
    }

    public InMemoryServiceOrderRepository(Consumer<Object> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ServiceOrder save(ServiceOrder serviceOrder) {
        if (!serviceOrders.contains(serviceOrder)) {
            ReflectionTestUtils.setField(serviceOrder, "id", nextId++);
            serviceOrders.add(serviceOrder);
        }
        serviceOrder.domainEvents().forEach(eventPublisher);
        serviceOrder.clearDomainEvents();
        return serviceOrder;
    }

    @Override
    public ServiceOrder saveAndFlush(ServiceOrder serviceOrder) {
        save(serviceOrder);
        Stream.concat(serviceOrder.getServiceItems().stream(), serviceOrder.getPartItems().stream())
                .filter(item -> item.getId() == null)
                .forEach(item -> ReflectionTestUtils.setField(item, "id", nextItemId++));
        return serviceOrder;
    }

    @Override
    public Optional<ServiceOrder> findById(Long id) {
        return serviceOrders.stream()
                .filter(serviceOrder -> serviceOrder.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<ServiceOrder> findByPublicIdAndCustomerId(UUID publicId, Long customerId) {
        return serviceOrders.stream()
                .filter(serviceOrder -> serviceOrder.getPublicId().equals(publicId))
                .filter(serviceOrder -> serviceOrder.getCustomerId().equals(customerId))
                .findFirst();
    }

    @Override
    public ServiceOrderNumber nextNumber() {
        return ServiceOrderNumber.of(Year.now(), nextSequence++);
    }

    @Override
    public Page<ServiceOrder> findPage(ServiceOrderFilter filter, Pagination pagination) {
        List<ServiceOrder> matching = serviceOrders.stream()
                .filter(serviceOrder -> filter.status() == null || serviceOrder.getStatus() == filter.status())
                .filter(serviceOrder ->
                        filter.customerId() == null || serviceOrder.getCustomerId().equals(filter.customerId()))
                .filter(serviceOrder ->
                        filter.vehicleId() == null || serviceOrder.getVehicleId().equals(filter.vehicleId()))
                .sorted(Comparator.comparing((ServiceOrder serviceOrder) -> serviceOrder.getStatus().isClosed())
                        .thenComparing(ServiceOrder::getId))
                .toList();
        List<ServiceOrder> content = matching.stream()
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, matching.size());
    }

    public List<ServiceOrder> findAll() {
        return List.copyOf(serviceOrders);
    }
}
