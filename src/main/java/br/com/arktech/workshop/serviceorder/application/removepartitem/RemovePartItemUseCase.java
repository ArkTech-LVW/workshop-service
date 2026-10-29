package br.com.arktech.workshop.serviceorder.application.removepartitem;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemovePartItemUseCase implements VoidUseCase<RemovePartItemCommand> {

    private final ServiceOrderRepository serviceOrderRepository;

    @Override
    @Transactional
    public void execute(RemovePartItemCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.removePartItem(command.itemId());
        serviceOrderRepository.save(serviceOrder);
    }
}
