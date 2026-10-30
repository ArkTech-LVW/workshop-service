package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.completeserviceitem.CompleteServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.completeserviceitem.CompleteServiceItemUseCase;
import br.com.arktech.workshop.serviceorder.application.completeserviceorder.CompleteServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.completeserviceorder.CompleteServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.deliverserviceorder.DeliverServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.deliverserviceorder.DeliverServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.reopenquote.ReopenQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.reopenquote.ReopenQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.sendquote.SendQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.sendquote.SendQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisCommand;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisUseCase;
import br.com.arktech.workshop.serviceorder.application.startexecution.StartExecutionCommand;
import br.com.arktech.workshop.serviceorder.application.startexecution.StartExecutionUseCase;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ServiceOrderController implements IServiceOrderController {

    private final OpenServiceOrderUseCase openServiceOrderUseCase;
    private final StartDiagnosisUseCase startDiagnosisUseCase;
    private final SendQuoteUseCase sendQuoteUseCase;
    private final ApproveQuoteUseCase approveQuoteUseCase;
    private final RejectQuoteUseCase rejectQuoteUseCase;
    private final ReopenQuoteUseCase reopenQuoteUseCase;
    private final StartExecutionUseCase startExecutionUseCase;
    private final CompleteServiceItemUseCase completeServiceItemUseCase;
    private final CompleteServiceOrderUseCase completeServiceOrderUseCase;
    private final DeliverServiceOrderUseCase deliverServiceOrderUseCase;
    private final CancelServiceOrderUseCase cancelServiceOrderUseCase;

    @Override
    public ServiceOrderResponse open(OpenServiceOrderRequest request) {
        ServiceOrderVehicleRequest vehicle = request.vehicle();
        OpenServiceOrderCommand command = new OpenServiceOrderCommand(
                request.customerDocument(),
                new VehicleDetails(
                        vehicle.licensePlate(), vehicle.brand(), vehicle.model(), vehicle.year(), vehicle.color()),
                request.problemDescription());
        return ServiceOrderResponse.from(openServiceOrderUseCase.execute(command));
    }

    @Override
    public ServiceOrderResponse startDiagnosis(Long id) {
        return ServiceOrderResponse.from(startDiagnosisUseCase.execute(new StartDiagnosisCommand(id)));
    }

    @Override
    public ServiceOrderResponse sendQuote(Long id) {
        return ServiceOrderResponse.from(sendQuoteUseCase.execute(new SendQuoteCommand(id)));
    }

    @Override
    public ServiceOrderResponse approve(Long id) {
        return ServiceOrderResponse.from(approveQuoteUseCase.execute(new ApproveQuoteCommand(id)));
    }

    @Override
    public ServiceOrderResponse reject(Long id, RejectQuoteRequest request) {
        return ServiceOrderResponse.from(rejectQuoteUseCase.execute(new RejectQuoteCommand(id, request.reason())));
    }

    @Override
    public ServiceOrderResponse reopenQuote(Long id, @Nullable ReopenQuoteRequest request) {
        String description = request == null ? null : request.description();
        return ServiceOrderResponse.from(reopenQuoteUseCase.execute(new ReopenQuoteCommand(id, description)));
    }

    @Override
    public ServiceOrderResponse startExecution(Long id) {
        return ServiceOrderResponse.from(startExecutionUseCase.execute(new StartExecutionCommand(id)));
    }

    @Override
    public ServiceOrderResponse completeService(Long id, Long itemId) {
        return ServiceOrderResponse.from(
                completeServiceItemUseCase.execute(new CompleteServiceItemCommand(id, itemId)));
    }

    @Override
    public ServiceOrderResponse complete(Long id) {
        return ServiceOrderResponse.from(completeServiceOrderUseCase.execute(new CompleteServiceOrderCommand(id)));
    }

    @Override
    public ServiceOrderResponse deliver(Long id) {
        return ServiceOrderResponse.from(deliverServiceOrderUseCase.execute(new DeliverServiceOrderCommand(id)));
    }

    @Override
    public ServiceOrderResponse cancel(Long id, CancelServiceOrderRequest request) {
        return ServiceOrderResponse.from(
                cancelServiceOrderUseCase.execute(new CancelServiceOrderCommand(id, request.reason())));
    }
}
