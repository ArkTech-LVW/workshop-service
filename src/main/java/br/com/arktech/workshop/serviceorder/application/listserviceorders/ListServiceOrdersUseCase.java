package br.com.arktech.workshop.serviceorder.application.listserviceorders;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderSummaryResult;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderFilter;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.registry.CustomerDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListServiceOrdersUseCase implements UseCase<ListServiceOrdersQuery, Page<ServiceOrderSummaryResult>> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CustomerDirectory customerDirectory;
    private final VehicleDirectory vehicleDirectory;

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderSummaryResult> execute(ListServiceOrdersQuery query) {
        Pagination pagination = new Pagination(query.page(), query.size());
        Optional<Long> customerId = query.customerDocument() == null
                ? Optional.empty()
                : customerDirectory.findIdByDocument(CPFCNPJ.of(query.customerDocument()));
        Optional<Long> vehicleId = query.licensePlate() == null
                ? Optional.empty()
                : vehicleDirectory.findIdByLicensePlate(new LicensePlate(query.licensePlate()));
        boolean unknownCustomer = query.customerDocument() != null && customerId.isEmpty();
        boolean unknownVehicle = query.licensePlate() != null && vehicleId.isEmpty();
        if (unknownCustomer || unknownVehicle) {
            return new Page<>(List.of(), pagination, 0);
        }

        ServiceOrderFilter filter = new ServiceOrderFilter(
                query.status(),
                customerId.orElse(null),
                vehicleId.orElse(null),
                query.openedFrom(),
                query.openedTo());
        return serviceOrderRepository.findPage(filter, pagination).map(ServiceOrderSummaryResult::from);
    }
}
