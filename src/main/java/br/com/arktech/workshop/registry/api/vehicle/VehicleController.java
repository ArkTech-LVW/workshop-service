package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleCommand;
import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleUseCase;
import br.com.arktech.workshop.registry.application.findvehicle.FindVehicleQuery;
import br.com.arktech.workshop.registry.application.findvehicle.FindVehicleUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class VehicleController implements IVehicleController {

    private final CreateVehicleUseCase createVehicleUseCase;
    private final FindVehicleUseCase findVehicleUseCase;

    @Override
    public VehicleResponse create(VehicleRequest request) {
        CreateVehicleCommand command = new CreateVehicleCommand(
                request.customerId(),
                request.licensePlate(),
                request.chassis(),
                request.brand(),
                request.model(),
                request.year(),
                request.color());
        return VehicleResponse.from(createVehicleUseCase.execute(command));
    }

    @Override
    public VehicleResponse findById(Long id) {
        return VehicleResponse.from(findVehicleUseCase.execute(new FindVehicleQuery(id)));
    }
}
