package br.com.arktech.workshop.inventory.api.stockmovement;

import br.com.arktech.workshop.inventory.application.liststockmovements.ListStockMovementsQuery;
import br.com.arktech.workshop.inventory.application.liststockmovements.ListStockMovementsUseCase;
import br.com.arktech.workshop.inventory.application.registerstockmovement.RegisterStockMovementCommand;
import br.com.arktech.workshop.inventory.application.registerstockmovement.RegisterStockMovementUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class StockMovementController implements IStockMovementController {

    private final RegisterStockMovementUseCase registerStockMovementUseCase;
    private final ListStockMovementsUseCase listStockMovementsUseCase;

    @Override
    public StockMovementResponse create(Long partId, StockMovementRequest request) {
        RegisterStockMovementCommand command =
                new RegisterStockMovementCommand(partId, request.type(), request.quantity(), request.reason());
        return StockMovementResponse.from(registerStockMovementUseCase.execute(command));
    }

    @Override
    public PageResponse<StockMovementResponse> list(Long partId, int page, int size) {
        ListStockMovementsQuery query = new ListStockMovementsQuery(partId, page, size);
        return PageResponse.from(listStockMovementsUseCase.execute(query), StockMovementResponse::from);
    }
}
