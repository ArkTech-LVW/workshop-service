package br.com.arktech.workshop.serviceorder.application.notifycustomer;

import br.com.arktech.workshop.shared.application.i18n.MoneyFormatter;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;
import br.com.arktech.workshop.shared.application.notification.Notifier;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class ServiceOrderNotificationListener {

    private final MoneyFormatter moneyFormatter;
    private final Notifier notifier;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(QuoteSentEvent event) {
        notifier.send(new Notification(
                event.customerId(),
                NotificationType.SERVICE_ORDER_QUOTE_SENT,
                List.of(event.publicId().toString(), moneyFormatter.format(event.totalPrice()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(QuoteApprovedEvent event) {
        notifier.send(new Notification(
                event.customerId(),
                NotificationType.SERVICE_ORDER_QUOTE_APPROVED,
                List.of(event.publicId().toString(), moneyFormatter.format(event.totalPrice()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(QuoteRejectedEvent event) {
        notifier.send(new Notification(
                event.customerId(),
                NotificationType.SERVICE_ORDER_QUOTE_REJECTED,
                List.of(event.publicId().toString(), moneyFormatter.format(event.totalPrice()), event.reason())));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ServiceOrderCompletedEvent event) {
        notifier.send(new Notification(
                event.customerId(),
                NotificationType.SERVICE_ORDER_COMPLETED,
                List.of(event.publicId().toString(), moneyFormatter.format(event.totalPrice()))));
    }
}
