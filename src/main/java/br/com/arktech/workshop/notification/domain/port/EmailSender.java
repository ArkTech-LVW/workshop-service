package br.com.arktech.workshop.notification.domain.port;

import br.com.arktech.workshop.shared.domain.email.Email;

import org.jspecify.annotations.Nullable;

public interface EmailSender {

    void send(Email recipient, String subject, String textBody, @Nullable String htmlBody);
}
