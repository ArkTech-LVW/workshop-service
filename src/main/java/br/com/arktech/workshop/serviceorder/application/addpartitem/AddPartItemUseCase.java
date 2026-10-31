package br.com.arktech.workshop.serviceorder.application.addpartitem;

import br.com.arktech.workshop.serviceorder.domain.model.PartItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.application.inventory.PartDirectory;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddPartItemUseCase implements UseCase<AddPartItemCommand, AddPartItemResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final PartDirectory partDirectory;

    @Override
    @Transactional
    public AddPartItemResult execute(AddPartItemCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        PartDetails part = partDirectory.detailsOf(command.partId());

        serviceOrder.addPartItem(command.partId(), command.quantity(), part.salePrice());
        partDirectory.requireAvailableFor(
                command.serviceOrderId(), command.partId(), quantityOf(serviceOrder, command.partId()));
        ServiceOrder saved = serviceOrderRepository.saveAndFlush(serviceOrder);

        return AddPartItemResult.from(saved.getPartItems().getLast(), saved.getTotalPrice(), part.availableQuantity());
    }

    private static int quantityOf(ServiceOrder serviceOrder, Long partId) {
        return serviceOrder.getPartItems().stream()
                .filter(item -> item.getPartId().equals(partId))
                .mapToInt(PartItem::getQuantity)
                .sum();
    }
}
