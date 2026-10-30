package br.com.arktech.workshop.notification.application.sendnotification;

import br.com.arktech.workshop.notification.domain.model.NotificationType;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
class ServiceOrderNotificationListener {

    private final SendNotificationUseCase sendNotificationUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteSentEvent event) {
        send(new SendNotificationCommand(
                NotificationType.QUOTE_SENT, event.publicId(), event.customerId(), event.totalPrice(), null));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteApprovedEvent event) {
        send(new SendNotificationCommand(
                NotificationType.QUOTE_APPROVED, event.publicId(), event.customerId(), event.totalPrice(), null));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteRejectedEvent event) {
        send(new SendNotificationCommand(
                NotificationType.QUOTE_REJECTED,
                event.publicId(),
                event.customerId(),
                event.totalPrice(),
                event.reason()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ServiceOrderCompletedEvent event) {
        send(new SendNotificationCommand(
                NotificationType.SERVICE_ORDER_COMPLETED,
                event.publicId(),
                event.customerId(),
                event.totalPrice(),
                null));
    }

    private void send(SendNotificationCommand command) {
        try {
            sendNotificationUseCase.execute(command);
        } catch (RuntimeException ex) {
            log.error(
                    "Notification failed [type={}, serviceOrderPublicId={}]",
                    command.type(),
                    command.serviceOrderPublicId(),
                    ex);
        }
    }
}
