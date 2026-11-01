package br.com.arktech.workshop.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChannelNotifierTest {

    @Test
    void shouldRecordTheNotificationInEveryChannel() {
        List<Notification> email = new ArrayList<>();
        List<Notification> whatsApp = new ArrayList<>();
        ChannelNotifier notifier = new ChannelNotifier(List.of(email::add, whatsApp::add));
        Notification notification =
                new Notification(10L, NotificationType.SERVICE_ORDER_COMPLETED, List.of("OS-1", "R$ 64,00"));

        notifier.send(notification);

        assertThat(email).containsExactly(notification);
        assertThat(whatsApp).containsExactly(notification);
    }
}
