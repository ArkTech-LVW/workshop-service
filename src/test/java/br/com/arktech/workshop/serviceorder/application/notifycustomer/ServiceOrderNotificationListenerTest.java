package br.com.arktech.workshop.serviceorder.application.notifycustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.application.i18n.MoneyFormatter;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteRejectedEvent;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteSentEvent;
import br.com.arktech.workshop.shared.event.serviceorder.ServiceOrderCompletedEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ServiceOrderNotificationListenerTest {

    private static final Long SERVICE_ORDER_ID = 1L;
    private static final UUID PUBLIC_ID = UUID.fromString("7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a");
    private static final Long CUSTOMER_ID = 10L;
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("1234.50");
    private static final String FORMATTED_TOTAL_PRICE = "R$ 1.234,50";

    private static final MoneyFormatter MONEY_FORMATTER = new MoneyFormatter(Locale.of("pt", "BR"));

    private final List<Notification> sentNotifications = new ArrayList<>();
    private final ServiceOrderNotificationListener listener =
            new ServiceOrderNotificationListener(MONEY_FORMATTER, sentNotifications::add);

    @Test
    void shouldNotifyTheCustomerOfEachServiceOrderEvent() {
        listener.on(new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, List.of()));
        listener.on(new QuoteApprovedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, List.of(), null));
        listener.on(new QuoteRejectedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, "Achou caro"));
        listener.on(new ServiceOrderCompletedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));

        String publicId = PUBLIC_ID.toString();
        assertThat(sentNotifications)
                .containsExactly(
                        new Notification(
                                CUSTOMER_ID,
                                NotificationType.SERVICE_ORDER_QUOTE_SENT,
                                List.of(publicId, FORMATTED_TOTAL_PRICE)),
                        new Notification(
                                CUSTOMER_ID,
                                NotificationType.SERVICE_ORDER_QUOTE_APPROVED,
                                List.of(publicId, FORMATTED_TOTAL_PRICE)),
                        new Notification(
                                CUSTOMER_ID,
                                NotificationType.SERVICE_ORDER_QUOTE_REJECTED,
                                List.of(publicId, FORMATTED_TOTAL_PRICE, "Achou caro")),
                        new Notification(
                                CUSTOMER_ID,
                                NotificationType.SERVICE_ORDER_COMPLETED,
                                List.of(publicId, FORMATTED_TOTAL_PRICE)));
    }

    @Test
    void shouldPropagateTheFailureSoTheActionIsRolledBack() {
        ServiceOrderNotificationListener failingListener =
                new ServiceOrderNotificationListener(MONEY_FORMATTER, notification -> {
                    throw new IllegalStateException("Notifier unavailable");
                });

        assertThatThrownBy(() -> failingListener.on(
                        new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, List.of())))
                .isInstanceOf(IllegalStateException.class);
    }
}
