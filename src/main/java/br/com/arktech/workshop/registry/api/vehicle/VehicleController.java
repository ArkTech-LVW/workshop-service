package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleCommand;
import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleUseCase;
import br.com.arktech.workshop.registry.application.deletevehicle.DeleteVehicleCommand;
import br.com.arktech.workshop.registry.application.deletevehicle.DeleteVehicleUseCase;
import br.com.arktech.workshop.registry.application.findvehicle.FindVehicleQuery;
import br.com.arktech.workshop.registry.application.findvehicle.FindVehicleUseCase;
import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesQuery;
import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesUseCase;
import br.com.arktech.workshop.registry.application.updatevehicle.UpdateVehicleCommand;
import br.com.arktech.workshop.registry.application.updatevehicle.UpdateVehicleUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class VehicleController implements IVehicleController {

    private final CreateVehicleUseCase createVehicleUseCase;
    private final ListVehiclesUseCase listVehiclesUseCase;
    private final FindVehicleUseCase findVehicleUseCase;
    private final UpdateVehicleUseCase updateVehicleUseCase;
    private final DeleteVehicleUseCase deleteVehicleUseCase;

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
    public PageResponse<VehicleResponse> list(@Nullable String licensePlate, int page, int size) {
        ListVehiclesQuery query = new ListVehiclesQuery(licensePlate, null, page, size);
        return PageResponse.from(listVehiclesUseCase.execute(query), VehicleResponse::from);
    }

    @Override
    public VehicleResponse findById(Long id) {
        return VehicleResponse.from(findVehicleUseCase.execute(new FindVehicleQuery(id)));
    }

    @Override
    public VehicleResponse update(Long id, UpdateVehicleRequest request) {
        UpdateVehicleCommand command =
                new UpdateVehicleCommand(id, request.licensePlate(), request.chassis(), request.color());
        return VehicleResponse.from(updateVehicleUseCase.execute(command));
    }

    @Override
    public void delete(Long id) {
        deleteVehicleUseCase.execute(new DeleteVehicleCommand(id));
    }
}
