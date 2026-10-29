package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisCommand;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisUseCase;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ServiceOrderController implements IServiceOrderController {

    private final OpenServiceOrderUseCase openServiceOrderUseCase;
    private final StartDiagnosisUseCase startDiagnosisUseCase;
    private final CancelServiceOrderUseCase cancelServiceOrderUseCase;

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

    @Override
    public ServiceOrderResponse startDiagnosis(Long id) {
        return ServiceOrderResponse.from(startDiagnosisUseCase.execute(new StartDiagnosisCommand(id)));
    }

    @Override
    public ServiceOrderResponse cancel(Long id, CancelServiceOrderRequest request) {
        return ServiceOrderResponse.from(
                cancelServiceOrderUseCase.execute(new CancelServiceOrderCommand(id, request.reason())));
    }
}
