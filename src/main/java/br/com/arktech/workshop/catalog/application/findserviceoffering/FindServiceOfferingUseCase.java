package br.com.arktech.workshop.catalog.application.findserviceoffering;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindServiceOfferingUseCase implements UseCase<FindServiceOfferingQuery, ServiceOfferingResult> {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional(readOnly = true)
    public ServiceOfferingResult execute(FindServiceOfferingQuery query) {
        return ServiceOfferingResult.from(serviceOfferingRepository.findRequiredById(query.id()));
    }
}
