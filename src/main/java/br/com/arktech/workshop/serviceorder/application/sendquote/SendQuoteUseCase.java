package br.com.arktech.workshop.serviceorder.application.sendquote;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendQuoteUseCase implements UseCase<SendQuoteCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ServiceOrderResult execute(SendQuoteCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        serviceOrder.sendQuote(currentUser.id(), null);
        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);
        serviceOrder.pullEvents().forEach(eventPublisher::publishEvent);
        return ServiceOrderResult.from(saved);
    }
}
