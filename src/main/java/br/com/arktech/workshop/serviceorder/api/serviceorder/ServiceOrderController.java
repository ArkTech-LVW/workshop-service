package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderUseCase;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ServiceOrderController implements IServiceOrderController {

    private final OpenServiceOrderUseCase openServiceOrderUseCase;

    @Override
    public ServiceOrderResponse open(OpenServiceOrderRequest request) {
        ServiceOrderVehicleRequest vehicle = request.vehicle();
        OpenServiceOrderCommand command = new OpenServiceOrderCommand(
                request.customerDocument(),
                new VehicleDetails(
                        vehicle.licensePlate(), vehicle.brand(), vehicle.model(), vehicle.year(), vehicle.color()),
                request.problemDescription());
        return ServiceOrderResponse.from(openServiceOrderUseCase.execute(command));
    }
}
