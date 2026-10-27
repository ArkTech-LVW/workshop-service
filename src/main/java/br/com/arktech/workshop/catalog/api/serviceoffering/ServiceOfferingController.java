package br.com.arktech.workshop.catalog.api.serviceoffering;

import br.com.arktech.workshop.catalog.application.activateserviceoffering.ActivateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.activateserviceoffering.ActivateServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.deactivateserviceoffering.DeactivateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.deactivateserviceoffering.DeactivateServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.findserviceoffering.FindServiceOfferingQuery;
import br.com.arktech.workshop.catalog.application.findserviceoffering.FindServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsQuery;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsUseCase;
import br.com.arktech.workshop.catalog.application.updateserviceoffering.UpdateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.updateserviceoffering.UpdateServiceOfferingUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ServiceOfferingController implements IServiceOfferingController {

    private final CreateServiceOfferingUseCase createServiceOfferingUseCase;
    private final ListServiceOfferingsUseCase listServiceOfferingsUseCase;
    private final FindServiceOfferingUseCase findServiceOfferingUseCase;
    private final UpdateServiceOfferingUseCase updateServiceOfferingUseCase;
    private final ActivateServiceOfferingUseCase activateServiceOfferingUseCase;
    private final DeactivateServiceOfferingUseCase deactivateServiceOfferingUseCase;

    @Override
    public ServiceOfferingResponse create(ServiceOfferingRequest request) {
        CreateServiceOfferingCommand command = new CreateServiceOfferingCommand(
                request.name(), request.description(), request.laborPrice(), request.estimatedMinutes());
        return ServiceOfferingResponse.from(createServiceOfferingUseCase.execute(command));
    }

    @Override
    public PageResponse<ServiceOfferingResponse> list(
            @Nullable String name, @Nullable Boolean active, int page, int size) {
        ListServiceOfferingsQuery query = new ListServiceOfferingsQuery(name, active, page, size);
        return PageResponse.from(listServiceOfferingsUseCase.execute(query), ServiceOfferingResponse::from);
    }

    @Override
    public ServiceOfferingResponse findById(Long id) {
        return ServiceOfferingResponse.from(findServiceOfferingUseCase.execute(new FindServiceOfferingQuery(id)));
    }

    @Override
    public ServiceOfferingResponse update(Long id, ServiceOfferingRequest request) {
        UpdateServiceOfferingCommand command = new UpdateServiceOfferingCommand(
                id, request.name(), request.description(), request.laborPrice(), request.estimatedMinutes());
        return ServiceOfferingResponse.from(updateServiceOfferingUseCase.execute(command));
    }

    @Override
    public ServiceOfferingResponse activate(Long id) {
        return ServiceOfferingResponse.from(
                activateServiceOfferingUseCase.execute(new ActivateServiceOfferingCommand(id)));
    }

    @Override
    public ServiceOfferingResponse deactivate(Long id) {
        return ServiceOfferingResponse.from(
                deactivateServiceOfferingUseCase.execute(new DeactivateServiceOfferingCommand(id)));
    }
}
