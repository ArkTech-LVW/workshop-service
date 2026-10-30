package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.api.serviceorder.RejectQuoteRequest;
import br.com.arktech.workshop.serviceorder.application.approvequoteascustomer.ApproveQuoteAsCustomerCommand;
import br.com.arktech.workshop.serviceorder.application.approvequoteascustomer.ApproveQuoteAsCustomerUseCase;
import br.com.arktech.workshop.serviceorder.application.rejectquoteascustomer.RejectQuoteAsCustomerCommand;
import br.com.arktech.workshop.serviceorder.application.rejectquoteascustomer.RejectQuoteAsCustomerUseCase;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderQuery;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderUseCase;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CustomerServiceOrderController implements ICustomerServiceOrderController {

    private final TrackServiceOrderUseCase trackServiceOrderUseCase;
    private final ApproveQuoteAsCustomerUseCase approveQuoteAsCustomerUseCase;
    private final RejectQuoteAsCustomerUseCase rejectQuoteAsCustomerUseCase;

    @Override
    public CustomerServiceOrderResponse track(UUID publicId, String customerDocument) {
        return CustomerServiceOrderResponse.from(
                trackServiceOrderUseCase.execute(new TrackServiceOrderQuery(publicId, customerDocument)));
    }

    @Override
    public void approve(UUID publicId, String customerDocument) {
        approveQuoteAsCustomerUseCase.execute(new ApproveQuoteAsCustomerCommand(publicId, customerDocument));
    }

    @Override
    public void reject(UUID publicId, String customerDocument, RejectQuoteRequest request) {
        rejectQuoteAsCustomerUseCase.execute(
                new RejectQuoteAsCustomerCommand(publicId, customerDocument, request.reason()));
    }
}
