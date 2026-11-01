package br.com.arktech.workshop.notification.application;

import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.Notifier;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ChannelNotifier implements Notifier {

    private final List<NotificationChannel> channels;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void send(Notification notification) {
        channels.forEach(channel -> channel.record(notification));
    }
}
