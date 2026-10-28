package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemCommand;
import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemUseCase;
import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemUseCase;
import br.com.arktech.workshop.serviceorder.application.removepartitem.RemovePartItemCommand;
import br.com.arktech.workshop.serviceorder.application.removepartitem.RemovePartItemUseCase;
import br.com.arktech.workshop.serviceorder.application.removeserviceitem.RemoveServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.removeserviceitem.RemoveServiceItemUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ServiceOrderItemController implements IServiceOrderItemController {

    private final AddServiceItemUseCase addServiceItemUseCase;
    private final RemoveServiceItemUseCase removeServiceItemUseCase;
    private final AddPartItemUseCase addPartItemUseCase;
    private final RemovePartItemUseCase removePartItemUseCase;

    @Override
    public ServiceItemResponse addService(Long id, AddServiceItemRequest request) {
        AddServiceItemCommand command = new AddServiceItemCommand(id, request.serviceOfferingId(), request.quantity());
        return ServiceItemResponse.from(addServiceItemUseCase.execute(command));
    }

    @Override
    public void removeService(Long id, Long itemId) {
        removeServiceItemUseCase.execute(new RemoveServiceItemCommand(id, itemId));
    }

    @Override
    public PartItemResponse addPart(Long id, AddPartItemRequest request) {
        AddPartItemCommand command = new AddPartItemCommand(id, request.partId(), request.quantity());
        return PartItemResponse.from(addPartItemUseCase.execute(command));
    }

    @Override
    public void removePart(Long id, Long itemId) {
        removePartItemUseCase.execute(new RemovePartItemCommand(id, itemId));
    }
}
