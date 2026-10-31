package br.com.arktech.workshop.serviceorder.application.approvequoteascustomer;

import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.shared.application.VoidUseCase;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveQuoteAsCustomerUseCase implements VoidUseCase<ApproveQuoteAsCustomerCommand> {

    private static final String INSUFFICIENT_STOCK = "part.insufficientStock";

    private final CustomerServiceOrderFinder customerServiceOrderFinder;
    private final ApproveQuoteUseCase approveQuoteUseCase;

    @Override
    @Transactional
    public void execute(ApproveQuoteAsCustomerCommand command) {
        ServiceOrder serviceOrder =
                customerServiceOrderFinder.findRequired(command.publicId(), command.customerDocument());
        try {
            approveQuoteUseCase.execute(new ApproveQuoteCommand(serviceOrder.getId()));
        } catch (BusinessRuleException ex) {
            if (INSUFFICIENT_STOCK.equals(ex.getCode())) {
                throw new BusinessRuleException("serviceOrder.partsUnavailable");
            }
            throw ex;
        }
    }
}
