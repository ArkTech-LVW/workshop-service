package br.com.arktech.workshop.notification.infrastructure.mail;

import br.com.arktech.workshop.notification.domain.port.EmailSender;
import br.com.arktech.workshop.shared.domain.email.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender, @Value("${notification.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(Email recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.value());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
