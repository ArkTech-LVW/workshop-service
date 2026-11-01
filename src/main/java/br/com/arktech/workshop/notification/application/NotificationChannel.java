package br.com.arktech.workshop.notification.application;

import br.com.arktech.workshop.shared.application.notification.Notification;

public interface NotificationChannel {

    void record(Notification notification);
}
