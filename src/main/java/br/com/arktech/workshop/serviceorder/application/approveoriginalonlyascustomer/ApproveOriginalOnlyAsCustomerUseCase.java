package br.com.arktech.workshop.serviceorder.application.approveoriginalonlyascustomer;

import br.com.arktech.workshop.serviceorder.application.approveoriginalonly.ApproveOriginalOnlyCommand;
import br.com.arktech.workshop.serviceorder.application.approveoriginalonly.ApproveOriginalOnlyUseCase;
import br.com.arktech.workshop.serviceorder.application.customerserviceorder.CustomerServiceOrderFinder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveOriginalOnlyAsCustomerUseCase implements VoidUseCase<ApproveOriginalOnlyAsCustomerCommand> {

    private final CustomerServiceOrderFinder customerServiceOrderFinder;
    private final ApproveOriginalOnlyUseCase approveOriginalOnlyUseCase;

    @Override
    @Transactional
    public void execute(ApproveOriginalOnlyAsCustomerCommand command) {
        ServiceOrder serviceOrder =
                customerServiceOrderFinder.findRequired(command.publicId(), command.customerDocument());
        approveOriginalOnlyUseCase.execute(new ApproveOriginalOnlyCommand(serviceOrder.getId()));
    }
}
