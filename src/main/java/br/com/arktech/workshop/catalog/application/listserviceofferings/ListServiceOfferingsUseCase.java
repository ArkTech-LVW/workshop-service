package br.com.arktech.workshop.catalog.application.listserviceofferings;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingFilter;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListServiceOfferingsUseCase implements UseCase<ListServiceOfferingsQuery, Page<ServiceOfferingResult>> {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOfferingResult> execute(ListServiceOfferingsQuery query) {
        ServiceOfferingFilter filter = new ServiceOfferingFilter(query.name(), query.active());
        Pagination pagination = new Pagination(query.page(), query.size());
        return serviceOfferingRepository.findPage(filter, pagination).map(ServiceOfferingResult::from);
    }
}
