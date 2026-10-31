package br.com.arktech.workshop.serviceorder.application.customerserviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerServiceOrderFinder {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CustomerDirectory customerDirectory;

    public ServiceOrder findRequired(UUID publicId, String customerDocument) {
        return customerDirectory
                .findIdByDocument(CPFCNPJ.of(customerDocument))
                .flatMap(customerId -> serviceOrderRepository.findByPublicIdAndCustomerId(publicId, customerId))
                .orElseThrow(() -> new NotFoundException("serviceOrder.notFound", publicId.toString()));
    }
}
