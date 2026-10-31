package br.com.arktech.workshop.serviceorder.application.notifycustomer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

import br.com.arktech.workshop.shared.application.i18n.MoneyFormatter;
import br.com.arktech.workshop.shared.application.i18n.TestMessageResolver;
import br.com.arktech.workshop.shared.application.notification.Notification;
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

    private static final MoneyFormatter MONEY_FORMATTER = new MoneyFormatter(Locale.of("pt", "BR"));

    private final List<Notification> sentNotifications = new ArrayList<>();
    private final ServiceOrderNotificationListener listener = new ServiceOrderNotificationListener(
            TestMessageResolver.withApplicationMessages(), MONEY_FORMATTER, sentNotifications::add);

    @Test
    void shouldNotifyTheCustomerOfEachServiceOrderEvent() {
        listener.on(new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));
        listener.on(new QuoteApprovedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, List.of(), null));
        listener.on(new QuoteRejectedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, "Achou caro"));
        listener.on(new ServiceOrderCompletedEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE));

        assertThat(sentNotifications)
                .extracting(Notification::customerId, Notification::message)
                .containsExactly(
                        tuple(
                                CUSTOMER_ID,
                                "O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi enviado "
                                        + "para aprovação. Valor total: R$ 1.234,50."),
                        tuple(
                                CUSTOMER_ID,
                                "O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi aprovado. "
                                        + "Valor total: R$ 1.234,50."),
                        tuple(
                                CUSTOMER_ID,
                                "O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi rejeitado. "
                                        + "Valor total: R$ 1.234,50. Motivo: Achou caro"),
                        tuple(
                                CUSTOMER_ID,
                                "A ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi finalizada e o veículo "
                                        + "está pronto para retirada. Valor total: R$ 1.234,50."));
    }

    @Test
    void shouldOnlyLogWhenTheNotificationFails() {
        ServiceOrderNotificationListener failingListener = new ServiceOrderNotificationListener(
                TestMessageResolver.withApplicationMessages(), MONEY_FORMATTER, notification -> {
                    throw new IllegalStateException("Notifier unavailable");
                });

        assertThatCode(() -> failingListener.on(
                        new QuoteSentEvent(SERVICE_ORDER_ID, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE)))
                .doesNotThrowAnyException();
    }
}
