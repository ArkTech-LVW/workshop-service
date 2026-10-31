package br.com.arktech.workshop.serviceorder.domain.repository;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Optional;

public interface ServiceOrderRepository {

    ServiceOrder save(ServiceOrder serviceOrder);

    ServiceOrder saveAndFlush(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(Long id);

    ServiceOrderNumber nextNumber();

    Page<ServiceOrder> findPage(ServiceOrderFilter filter, Pagination pagination);

    default ServiceOrder findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("serviceOrder.notFound", id.toString()));
    }
}
