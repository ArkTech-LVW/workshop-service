package br.com.arktech.workshop.serviceorder.application.approvequoteascustomer;

import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveQuoteAsCustomerUseCase implements VoidUseCase<ApproveQuoteAsCustomerCommand> {

    private final CustomerServiceOrderFinder customerServiceOrderFinder;
    private final ApproveQuoteUseCase approveQuoteUseCase;

    @Override
    @Transactional
    public void execute(ApproveQuoteAsCustomerCommand command) {
        ServiceOrder serviceOrder =
                customerServiceOrderFinder.findRequired(command.publicId(), command.customerDocument());
        approveQuoteUseCase.execute(new ApproveQuoteCommand(serviceOrder.getId()));
    }
}
