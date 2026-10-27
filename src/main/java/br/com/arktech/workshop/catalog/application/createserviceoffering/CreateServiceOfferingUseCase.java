package br.com.arktech.workshop.catalog.application.createserviceoffering;

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
public class CreateServiceOfferingUseCase implements UseCase<CreateServiceOfferingCommand, ServiceOfferingResult> {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional
    public ServiceOfferingResult execute(CreateServiceOfferingCommand command) {
        ServiceOffering serviceOffering = new ServiceOffering(
                command.name(), command.description(), new Money(command.laborPrice()), command.estimatedMinutes());
        return ServiceOfferingResult.from(serviceOfferingRepository.save(serviceOffering));
    }
}
