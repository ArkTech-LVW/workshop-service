package br.com.arktech.workshop.serviceorder.application.openserviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpenServiceOrderUseCase implements UseCase<OpenServiceOrderCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CustomerDirectory customerDirectory;
    private final VehicleDirectory vehicleDirectory;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public ServiceOrderResult execute(OpenServiceOrderCommand command) {
        CPFCNPJ document = CPFCNPJ.of(command.customerDocument());
        Long customerId = customerDirectory
                .findIdByDocument(document)
                .orElseThrow(() -> new NotFoundException("serviceOrder.customerNotRegistered", document.formatted()));
        Long vehicleId = vehicleDirectory.findOrRegister(customerId, command.vehicle());

        ServiceOrder serviceOrder = new ServiceOrder(
                serviceOrderRepository.nextNumber(),
                customerId,
                vehicleId,
                command.problemDescription(),
                currentUser.id());
        return ServiceOrderResult.from(serviceOrderRepository.save(serviceOrder));
    }
}
