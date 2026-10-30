package br.com.arktech.workshop.serviceorder.application.approvequote;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveQuoteUseCase implements UseCase<ApproveQuoteCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public ServiceOrderResult execute(ApproveQuoteCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.approveQuote(currentUser.findId().orElse(null), null);

        return ServiceOrderResult.from(serviceOrderRepository.save(serviceOrder));
    }
}
