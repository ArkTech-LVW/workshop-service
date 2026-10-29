package br.com.arktech.workshop.serviceorder.domain.repository;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryServiceOrderRepository implements ServiceOrderRepository {

    private final List<ServiceOrder> serviceOrders = new ArrayList<>();

    private long nextId = 1;

    private long nextSequence = 1;

    @Override
    public ServiceOrder save(ServiceOrder serviceOrder) {
        if (!serviceOrders.contains(serviceOrder)) {
            ReflectionTestUtils.setField(serviceOrder, "id", nextId++);
            serviceOrders.add(serviceOrder);
        }
        return serviceOrder;
    }

    @Override
    public Optional<ServiceOrder> findById(Long id) {
        return serviceOrders.stream()
                .filter(serviceOrder -> serviceOrder.getId().equals(id))
                .findFirst();
    }

    @Override
    public ServiceOrderNumber nextNumber() {
        return ServiceOrderNumber.of(Year.now(), nextSequence++);
    }

    public List<ServiceOrder> findAll() {
        return List.copyOf(serviceOrders);
    }
}
