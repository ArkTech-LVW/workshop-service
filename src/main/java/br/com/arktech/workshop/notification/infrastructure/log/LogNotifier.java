package br.com.arktech.workshop.notification.infrastructure.log;

import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.Notifier;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
class LogNotifier implements Notifier {

    @Override
    public void send(Notification notification) {
        log.info(
                "Notification [customerId={}, type={}] {}",
                notification.customerId(),
                notification.type(),
                notification.parameters());
    }
}
