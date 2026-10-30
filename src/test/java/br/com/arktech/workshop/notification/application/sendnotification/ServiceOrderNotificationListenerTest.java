package br.com.arktech.workshop.notification.application.sendnotification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.notification.domain.model.Notification;
import br.com.arktech.workshop.notification.domain.model.NotificationType;
import br.com.arktech.workshop.shared.application.i18n.TestMessageResolver;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ServiceOrderNotificationListenerTest {

    private static final Long SERVICE_ORDER_ID = 1L;
    private static final UUID PUBLIC_ID = UUID.randomUUID();
    private static final Long CUSTOMER_ID = 10L;
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("300.00");

    private final List<Notification> sentNotifications = new ArrayList<>();
    private final ServiceOrderNotificationListener listener = new ServiceOrderNotificationListener(
            new SendNotificationUseCase(TestMessageResolver.withApplicationMessages(), sentNotifications::add));

    @Test
    void shouldNotifyEachServiceOrderEvent() {
        listener.on(new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));
        listener.on(new QuoteApprovedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, List.of(), null));
        listener.on(new QuoteRejectedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, "Achou caro"));
        listener.on(new ServiceOrderCompletedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));

        assertThat(sentNotifications)
                .extracting(
                        Notification::type,
                        Notification::serviceOrderPublicId,
                        Notification::customerId,
                        Notification::totalPrice)
                .containsExactly(
                        tuple(NotificationType.QUOTE_SENT, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE),
                        tuple(NotificationType.QUOTE_APPROVED, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE),
                        tuple(NotificationType.QUOTE_REJECTED, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE),
                        tuple(NotificationType.SERVICE_ORDER_COMPLETED, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));
        assertThat(sentNotifications.get(2).message()).endsWith("Motivo: Achou caro");
    }

    @Test
    void shouldOnlyLogWhenTheNotificationFails() {
        ServiceOrderNotificationListener failingListener = new ServiceOrderNotificationListener(
                new SendNotificationUseCase(TestMessageResolver.withApplicationMessages(), notification -> {
                    throw new IllegalStateException("Notifier unavailable");
                }));

        assertThatCode(() -> failingListener.on(
                        new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE)))
                .doesNotThrowAnyException();
    }
}
