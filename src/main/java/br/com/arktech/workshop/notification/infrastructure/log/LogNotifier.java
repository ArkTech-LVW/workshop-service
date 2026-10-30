package br.com.arktech.workshop.notification.infrastructure.log;

import br.com.arktech.workshop.notification.application.port.Notifier;
import br.com.arktech.workshop.notification.domain.model.Notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
class LogNotifier implements Notifier {

    @Override
    public void send(Notification notification) {
        log.info(
                "Notification [type={}, serviceOrderPublicId={}, customerId={}, totalPrice={}] {}",
                notification.type(),
                notification.serviceOrderPublicId(),
                notification.customerId(),
                notification.totalPrice(),
                notification.message());
    }
}
