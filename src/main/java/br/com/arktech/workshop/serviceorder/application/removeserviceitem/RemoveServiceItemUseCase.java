package br.com.arktech.workshop.serviceorder.application.removeserviceitem;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveServiceItemUseCase implements VoidUseCase<RemoveServiceItemCommand> {

    private final ServiceOrderRepository serviceOrderRepository;

    @Override
    @Transactional
    public void execute(RemoveServiceItemCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.removeServiceItem(command.itemId());
        serviceOrderRepository.save(serviceOrder);
    }
}
