package br.com.arktech.workshop.notification.application.sendnotification;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.notification.domain.model.Notification;
import br.com.arktech.workshop.notification.domain.model.NotificationType;
import br.com.arktech.workshop.shared.application.i18n.TestMessageResolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SendNotificationUseCaseTest {

    private static final UUID PUBLIC_ID = UUID.fromString("7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a");
    private static final Long CUSTOMER_ID = 10L;
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("1234.50");

    private final List<Notification> sentNotifications = new ArrayList<>();
    private final SendNotificationUseCase useCase =
            new SendNotificationUseCase(TestMessageResolver.withApplicationMessages(), sentNotifications::add);

    @Test
    void shouldSendTheNotificationWithTheServiceOrderData() {
        useCase.execute(command(NotificationType.QUOTE_SENT, null));

        assertThat(sentNotifications).singleElement().satisfies(notification -> {
            assertThat(notification.type()).isEqualTo(NotificationType.QUOTE_SENT);
            assertThat(notification.serviceOrderPublicId()).isEqualTo(PUBLIC_ID);
            assertThat(notification.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(notification.totalPrice()).isEqualTo(TOTAL_PRICE);
        });
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            QUOTE_SENT              | O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi enviado para aprovação. Valor total: R$ 1.234,50.
            QUOTE_APPROVED          | O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi aprovado. Valor total: R$ 1.234,50.
            SERVICE_ORDER_COMPLETED | A ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi finalizada e o veículo está pronto para retirada. Valor total: R$ 1.234,50.
            """)
    void shouldWriteTheMessageOfEachType(NotificationType type, String message) {
        useCase.execute(command(type, null));

        assertThat(sentNotifications).singleElement().extracting(Notification::message).isEqualTo(message);
    }

    @Test
    void shouldIncludeTheReasonWhenTheQuoteIsRejected() {
        useCase.execute(command(NotificationType.QUOTE_REJECTED, "Achou caro"));

        assertThat(sentNotifications)
                .singleElement()
                .extracting(Notification::message)
                .isEqualTo("O orçamento da ordem de serviço 7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a foi rejeitado. "
                        + "Valor total: R$ 1.234,50. Motivo: Achou caro");
    }

    private static SendNotificationCommand command(NotificationType type, @Nullable String reason) {
        return new SendNotificationCommand(type, PUBLIC_ID, CUSTOMER_ID, TOTAL_PRICE, reason);
    }
}
