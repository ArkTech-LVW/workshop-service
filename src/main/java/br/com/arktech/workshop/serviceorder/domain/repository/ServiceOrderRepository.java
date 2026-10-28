package br.com.arktech.workshop.serviceorder.domain.repository;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.util.Optional;

public interface ServiceOrderRepository {

    ServiceOrder save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(Long id);

    ServiceOrderNumber nextNumber();

    default ServiceOrder findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("serviceOrder.notFound", id.toString()));
    }
}
