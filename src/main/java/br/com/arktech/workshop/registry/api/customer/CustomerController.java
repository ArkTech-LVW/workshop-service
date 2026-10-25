package br.com.arktech.workshop.registry.api.customer;

import br.com.arktech.workshop.registry.application.createcustomer.CreateCustomerCommand;
import br.com.arktech.workshop.registry.application.createcustomer.CreateCustomerUseCase;
import br.com.arktech.workshop.registry.application.findcustomer.FindCustomerQuery;
import br.com.arktech.workshop.registry.application.findcustomer.FindCustomerUseCase;
import br.com.arktech.workshop.registry.application.listcustomers.ListCustomersQuery;
import br.com.arktech.workshop.registry.application.listcustomers.ListCustomersUseCase;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CustomerController implements ICustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final ListCustomersUseCase listCustomersUseCase;
    private final FindCustomerUseCase findCustomerUseCase;

    @Override
    public CustomerResponse create(CustomerRequest request) {
        CreateCustomerCommand command =
                new CreateCustomerCommand(request.name(), request.document(), request.email(), request.phone());
        return CustomerResponse.from(createCustomerUseCase.execute(command));
    }

    @Override
    public PageResponse<CustomerResponse> list(@Nullable String document, int page, int size) {
        ListCustomersQuery query = new ListCustomersQuery(document, page, size);
        return PageResponse.from(listCustomersUseCase.execute(query), CustomerResponse::from);
    }

    @Override
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(findCustomerUseCase.execute(new FindCustomerQuery(id)));
    }
}
