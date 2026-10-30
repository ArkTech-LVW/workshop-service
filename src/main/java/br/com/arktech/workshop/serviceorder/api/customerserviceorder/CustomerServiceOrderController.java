package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderQuery;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderUseCase;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CustomerServiceOrderController implements ICustomerServiceOrderController {

    private final TrackServiceOrderUseCase trackServiceOrderUseCase;

    @Override
    public CustomerServiceOrderResponse track(UUID publicId, String customerDocument) {
        return CustomerServiceOrderResponse.from(
                trackServiceOrderUseCase.execute(new TrackServiceOrderQuery(publicId, customerDocument)));
    }
}
