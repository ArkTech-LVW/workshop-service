package br.com.arktech.workshop.notification.infrastructure.mail;

import br.com.arktech.workshop.notification.domain.port.EmailSender;
import br.com.arktech.workshop.shared.domain.email.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void send(Email recipient, String subject, String textBody, @Nullable String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, htmlBody != null, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient.value());
            helper.setSubject(subject);
            if (htmlBody != null) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(textBody);
            }
        } catch (MessagingException ex) {
            throw new MailPreparationException("Could not prepare the email to " + recipient.value(), ex);
        }
        mailSender.send(message);
    }
}
