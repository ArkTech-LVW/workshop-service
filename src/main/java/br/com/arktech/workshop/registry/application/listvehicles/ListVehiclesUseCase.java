package br.com.arktech.workshop.registry.application.listvehicles;

import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.VehicleFilter;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListVehiclesUseCase implements UseCase<ListVehiclesQuery, Page<VehicleResult>> {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResult> execute(ListVehiclesQuery query) {
        if (query.customerId() != null) {
            customerRepository.findRequiredById(query.customerId());
        }

        LicensePlate licensePlate = query.licensePlate() == null ? null : new LicensePlate(query.licensePlate());
        VehicleFilter filter = new VehicleFilter(licensePlate, query.customerId());
        Pagination pagination = new Pagination(query.page(), query.size());
        return vehicleRepository.findPage(filter, pagination).map(VehicleResult::from);
    }
}
