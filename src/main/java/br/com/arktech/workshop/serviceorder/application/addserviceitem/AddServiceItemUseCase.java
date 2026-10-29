package br.com.arktech.workshop.serviceorder.application.addserviceitem;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.catalog.ServiceOfferingDirectory;
import br.com.arktech.workshop.shared.domain.money.Money;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddServiceItemUseCase implements UseCase<AddServiceItemCommand, AddServiceItemResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOfferingDirectory serviceOfferingDirectory;

    @Override
    @Transactional
    public AddServiceItemResult execute(AddServiceItemCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        Money laborPrice = serviceOfferingDirectory.laborPriceOf(command.serviceOfferingId());

        serviceOrder.addServiceItem(command.serviceOfferingId(), command.quantity(), laborPrice);
        ServiceOrder saved = serviceOrderRepository.saveAndFlush(serviceOrder);
        return AddServiceItemResult.from(saved.getServiceItems().getLast(), saved.getTotalPrice());
    }
}
