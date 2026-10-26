package br.com.arktech.workshop.catalog.application.deactivateserviceoffering;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateServiceOfferingUseCase
        implements UseCase<DeactivateServiceOfferingCommand, ServiceOfferingResult> {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional
    public ServiceOfferingResult execute(DeactivateServiceOfferingCommand command) {
        ServiceOffering serviceOffering = serviceOfferingRepository.findRequiredById(command.id());

        serviceOffering.deactivate();
        return ServiceOfferingResult.from(serviceOfferingRepository.save(serviceOffering));
    }
}
