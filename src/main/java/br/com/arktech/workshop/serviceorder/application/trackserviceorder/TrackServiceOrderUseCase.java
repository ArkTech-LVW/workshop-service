package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.PartItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceItem;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.catalog.ServiceOfferingDirectory;
import br.com.arktech.workshop.shared.application.inventory.PartDirectory;
import br.com.arktech.workshop.shared.application.registry.VehicleDirectory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackServiceOrderUseCase implements UseCase<TrackServiceOrderQuery, TrackServiceOrderResult> {

    private final CustomerServiceOrderFinder customerServiceOrderFinder;
    private final VehicleDirectory vehicleDirectory;
    private final ServiceOfferingDirectory serviceOfferingDirectory;
    private final PartDirectory partDirectory;

    @Override
    @Transactional(readOnly = true)
    public TrackServiceOrderResult execute(TrackServiceOrderQuery query) {
        ServiceOrder serviceOrder = customerServiceOrderFinder.findRequired(query.publicId(), query.customerDocument());
        return new TrackServiceOrderResult(
                serviceOrder.getPublicId(),
                serviceOrder.getNumber().value(),
                serviceOrder.getStatus(),
                serviceOrder.getProblemDescription(),
                vehicleDirectory.detailsOf(serviceOrder.getVehicleId()),
                serviceOrder.getStatusHistory().stream().map(StatusChangeResult::from).toList(),
                quoteOf(serviceOrder),
                serviceOrder.getDateCreated());
    }

    private @Nullable QuoteResult quoteOf(ServiceOrder serviceOrder) {
        if (!serviceOrder.getStatus().showsQuoteToCustomer()) {
            return null;
        }

        List<ServiceItem> serviceItems = serviceOrder.getServiceItems();
        List<PartItem> partItems = serviceOrder.getPartItems();
        Map<Long, String> serviceNames = serviceOfferingDirectory.namesOf(serviceItems.stream().map(ServiceItem::getServiceOfferingId).toList());
        Map<Long, String> partNames = partDirectory.namesOf(partItems.stream().map(PartItem::getPartId).toList());

        return new QuoteResult(
                serviceItems.stream()
                        .map(item -> QuoteItemResult.of(nameOf(serviceNames, item.getServiceOfferingId()), item))
                        .toList(),
                partItems.stream()
                        .map(item -> QuoteItemResult.of(nameOf(partNames, item.getPartId()), item))
                        .toList(),
                serviceOrder.getServicesTotal().amount(),
                serviceOrder.getPartsTotal().amount(),
                serviceOrder.getTotalPrice().amount());
    }

    private static String nameOf(Map<Long, String> names, Long id) {
        return Objects.requireNonNull(names.get(id));
    }
}
