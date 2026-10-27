package br.com.arktech.workshop.catalog.api.serviceoffering;

import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.findserviceoffering.FindServiceOfferingQuery;
import br.com.arktech.workshop.catalog.application.findserviceoffering.FindServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsQuery;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsUseCase;
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

    @Override
    public ServiceOfferingResponse create(ServiceOfferingRequest request) {
        CreateServiceOfferingCommand command = new CreateServiceOfferingCommand(
                request.name(), request.description(), request.laborPrice(), request.estimatedMinutes());
        return ServiceOfferingResponse.from(createServiceOfferingUseCase.execute(command));
    }

    @Override
    public PageResponse<ServiceOfferingResponse> list(@Nullable String name, int page, int size) {
        ListServiceOfferingsQuery query = new ListServiceOfferingsQuery(name, page, size);
        return PageResponse.from(listServiceOfferingsUseCase.execute(query), ServiceOfferingResponse::from);
    }

    @Override
    public ServiceOfferingResponse findById(Long id) {
        return ServiceOfferingResponse.from(findServiceOfferingUseCase.execute(new FindServiceOfferingQuery(id)));
    }
}
