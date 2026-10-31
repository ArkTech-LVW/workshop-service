package br.com.arktech.workshop.serviceorder.application.notifycustomer;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.Notifier;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import java.util.UUID;

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

    private final MessageResolver messageResolver;
    private final Notifier notifier;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteSentEvent event) {
        notify(
                event.publicId(),
                event.customerId(),
                "notification.serviceOrder.quoteSent",
                event.publicId(),
                event.totalPrice());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteApprovedEvent event) {
        notify(
                event.publicId(),
                event.customerId(),
                "notification.serviceOrder.quoteApproved",
                event.publicId(),
                event.totalPrice());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(QuoteRejectedEvent event) {
        notify(
                event.publicId(),
                event.customerId(),
                "notification.serviceOrder.quoteRejected",
                event.publicId(),
                event.totalPrice(),
                event.reason());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ServiceOrderCompletedEvent event) {
        notify(
                event.publicId(),
                event.customerId(),
                "notification.serviceOrder.completed",
                event.publicId(),
                event.totalPrice());
    }

    private void notify(UUID serviceOrderPublicId, Long customerId, String messageKey, Object... messageArgs) {
        try {
            notifier.send(new Notification(customerId, messageResolver.get(messageKey, messageArgs)));
        } catch (RuntimeException ex) {
            log.error(
                    "Notification failed [messageKey={}, serviceOrderPublicId={}]",
                    messageKey,
                    serviceOrderPublicId,
                    ex);
        }
    }
}
