package br.com.arktech.workshop.catalog.application.updateserviceoffering;

import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.money.Money;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateServiceOfferingUseCase implements UseCase<UpdateServiceOfferingCommand, ServiceOfferingResult> {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional
    public ServiceOfferingResult execute(UpdateServiceOfferingCommand command) {
        ServiceOffering serviceOffering = serviceOfferingRepository.findRequiredById(command.id());

        serviceOffering.rename(command.name());
        serviceOffering.describe(command.description());
        serviceOffering.changeLaborPrice(new Money(command.laborPrice()));
        serviceOffering.changeEstimatedMinutes(command.estimatedMinutes());
        return ServiceOfferingResult.from(serviceOfferingRepository.save(serviceOffering));
    }
}
