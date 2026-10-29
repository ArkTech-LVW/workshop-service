package br.com.arktech.workshop.catalog.application.serviceoffering;

import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.shared.application.catalog.ServiceOfferingDirectory;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.money.Money;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class CatalogServiceOfferingDirectory implements ServiceOfferingDirectory {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    public Money laborPriceOf(Long serviceOfferingId) {
        ServiceOffering serviceOffering = serviceOfferingRepository.findRequiredById(serviceOfferingId);
        if (!serviceOffering.isActive()) {
            throw new BusinessRuleException("serviceOffering.inactive", serviceOfferingId.toString());
        }

        return serviceOffering.getLaborPrice();
    }
}
