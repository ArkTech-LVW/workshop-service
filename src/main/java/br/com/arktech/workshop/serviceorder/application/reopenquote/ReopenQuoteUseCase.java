package br.com.arktech.workshop.serviceorder.application.reopenquote;

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
public class ReopenQuoteUseCase implements UseCase<ReopenQuoteCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public ServiceOrderResult execute(ReopenQuoteCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.reopenQuote(currentUser.id(), command.description());

        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);

        return ServiceOrderResult.from(saved);
    }
}
