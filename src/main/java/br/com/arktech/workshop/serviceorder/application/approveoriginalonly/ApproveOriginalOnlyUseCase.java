package br.com.arktech.workshop.serviceorder.application.approveoriginalonly;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveOriginalOnlyUseCase implements UseCase<ApproveOriginalOnlyCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;
    private final MessageResolver messageResolver;

    @Override
    @Transactional
    public ServiceOrderResult execute(ApproveOriginalOnlyCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.approveOriginalOnly(
                currentUser.findId().orElse(null), messageResolver.get("serviceOrder.originalOnlyApproved"));

        return ServiceOrderResult.from(serviceOrderRepository.save(serviceOrder));
    }
}
