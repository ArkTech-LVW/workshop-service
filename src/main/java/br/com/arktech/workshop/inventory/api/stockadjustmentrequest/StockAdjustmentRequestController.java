package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest.ApproveStockAdjustmentRequestCommand;
import br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest.ApproveStockAdjustmentRequestUseCase;
import br.com.arktech.workshop.inventory.application.liststockadjustmentrequests.ListStockAdjustmentRequestsQuery;
import br.com.arktech.workshop.inventory.application.liststockadjustmentrequests.ListStockAdjustmentRequestsUseCase;
import br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest.RejectStockAdjustmentRequestCommand;
import br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest.RejectStockAdjustmentRequestUseCase;
import br.com.arktech.workshop.inventory.application.requeststockadjustment.RequestStockAdjustmentCommand;
import br.com.arktech.workshop.inventory.application.requeststockadjustment.RequestStockAdjustmentUseCase;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class StockAdjustmentRequestController implements IStockAdjustmentRequestController {

    private final RequestStockAdjustmentUseCase requestStockAdjustmentUseCase;
    private final ListStockAdjustmentRequestsUseCase listStockAdjustmentRequestsUseCase;
    private final ApproveStockAdjustmentRequestUseCase approveStockAdjustmentRequestUseCase;
    private final RejectStockAdjustmentRequestUseCase rejectStockAdjustmentRequestUseCase;

    @Override
    public StockAdjustmentRequestResponse create(Long partId, CreateStockAdjustmentRequestRequest request) {
        RequestStockAdjustmentCommand command =
                new RequestStockAdjustmentCommand(partId, request.quantity(), request.reason());
        return StockAdjustmentRequestResponse.from(requestStockAdjustmentUseCase.execute(command));
    }

    @Override
    public PageResponse<StockAdjustmentRequestResponse> list(
            @Nullable StockAdjustmentRequestStatus status, int page, int size) {
        ListStockAdjustmentRequestsQuery query = new ListStockAdjustmentRequestsQuery(status, page, size);
        return PageResponse.from(
                listStockAdjustmentRequestsUseCase.execute(query), StockAdjustmentRequestResponse::from);
    }

    @Override
    public StockAdjustmentRequestResponse approve(Long id) {
        return StockAdjustmentRequestResponse.from(
                approveStockAdjustmentRequestUseCase.execute(new ApproveStockAdjustmentRequestCommand(id)));
    }

    @Override
    public StockAdjustmentRequestResponse reject(Long id, RejectStockAdjustmentRequestRequest request) {
        return StockAdjustmentRequestResponse.from(rejectStockAdjustmentRequestUseCase.execute(
                new RejectStockAdjustmentRequestCommand(id, request.reviewNote())));
    }
}
