package br.com.arktech.workshop.catalog.domain.repository;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository {

    ServiceOffering save(ServiceOffering serviceOffering);

    Optional<ServiceOffering> findById(Long id);

    List<ServiceOffering> findAllById(Iterable<Long> ids);

    Page<ServiceOffering> findPage(ServiceOfferingFilter filter, Pagination pagination);

    default ServiceOffering findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("serviceOffering.notFound", id.toString()));
    }
}
