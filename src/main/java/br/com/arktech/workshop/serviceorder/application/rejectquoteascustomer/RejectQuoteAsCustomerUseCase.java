package br.com.arktech.workshop.serviceorder.application.rejectquoteascustomer;

import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteUseCase;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectQuoteAsCustomerUseCase implements VoidUseCase<RejectQuoteAsCustomerCommand> {

    private final CustomerServiceOrderFinder customerServiceOrderFinder;
    private final RejectQuoteUseCase rejectQuoteUseCase;

    @Override
    @Transactional
    public void execute(RejectQuoteAsCustomerCommand command) {
        ServiceOrder serviceOrder =
                customerServiceOrderFinder.findRequired(command.publicId(), command.customerDocument());
        rejectQuoteUseCase.execute(new RejectQuoteCommand(serviceOrder.getId(), command.reason()));
    }
}
