package br.com.arktech.workshop.serviceorder.application.approvequote;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.application.security.CurrentUser;
import br.com.arktech.workshop.shared.application.serviceorder.QuoteApprovedEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inventory withdraws the parts while handling {@link QuoteApprovedEvent} in this same transaction, so a part
 * without enough stock rolls back the approval as a whole.
 */
@Service
@RequiredArgsConstructor
public class ApproveQuoteUseCase implements UseCase<ApproveQuoteCommand, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ServiceOrderResult execute(ApproveQuoteCommand command) {
        ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(command.serviceOrderId());
        Long userId = currentUser.findId().orElse(null);

        serviceOrder.approveQuote(userId, null);
        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);

        eventPublisher.publishEvent(new QuoteApprovedEvent(
                saved.getId(),
                saved.getPublicId(),
                saved.getPartItems().stream()
                        .map(item -> new QuoteApprovedEvent.PartQuantity(item.getPartId(), item.getQuantity()))
                        .toList(),
                userId));
        return ServiceOrderResult.from(saved);
    }
}
