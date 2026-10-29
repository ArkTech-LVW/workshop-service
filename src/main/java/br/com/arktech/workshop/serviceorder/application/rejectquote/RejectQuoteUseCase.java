package br.com.arktech.workshop.serviceorder.application.rejectquote;

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
public class RejectQuoteUseCase implements UseCase<RejectQuoteCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public ServiceOrderResult execute(RejectQuoteCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.rejectQuote(currentUser.findId().orElse(null), command.reason());

        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);

        return ServiceOrderResult.from(saved);
    }
}
