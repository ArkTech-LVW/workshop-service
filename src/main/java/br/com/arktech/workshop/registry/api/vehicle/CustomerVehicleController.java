package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesQuery;
import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CustomerVehicleController implements ICustomerVehicleController {

    private final ListVehiclesUseCase listVehiclesUseCase;

    @Override
    public PageResponse<VehicleResponse> listByCustomer(Long customerId, int page, int size) {
        ListVehiclesQuery query = new ListVehiclesQuery(null, customerId, page, size);
        return PageResponse.from(listVehiclesUseCase.execute(query), VehicleResponse::from);
    }
}
