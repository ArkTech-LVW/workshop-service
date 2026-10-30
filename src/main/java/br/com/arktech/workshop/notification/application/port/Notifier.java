package br.com.arktech.workshop.notification.application.port;

import br.com.arktech.workshop.notification.domain.model.Notification;

public interface Notifier {

    void send(Notification notification);
}
