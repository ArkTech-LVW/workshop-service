package br.com.arktech.workshop.notification.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.email.Email;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "email_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailNotification extends BaseEntity {

    private Long customerId;

    private String type;

    private @Nullable Email recipient;

    private String subject;

    private String body;

    private @Nullable String htmlBody;

    @Enumerated(EnumType.STRING)
    private EmailNotificationStatus status;

    private int attempts;

    private @Nullable Instant nextAttemptAt;

    private @Nullable Instant sentAt;

    private @Nullable String lastError;

    private EmailNotification(
            Long customerId,
            String type,
            @Nullable Email recipient,
            String subject,
            String body,
            @Nullable String htmlBody,
            EmailNotificationStatus status,
            @Nullable Instant nextAttemptAt,
            @Nullable String lastError) {
        this.customerId = customerId;
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.htmlBody = htmlBody;
        this.status = status;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
    }

    public static EmailNotification pending(
            Long customerId,
            String type,
            Email recipient,
            String subject,
            String body,
            @Nullable String htmlBody,
            Instant now) {
        return new EmailNotification(
                customerId, type, recipient, subject, body, htmlBody, EmailNotificationStatus.PENDING, now, null);
    }

    public static EmailNotification withoutRecipient(
            Long customerId, String type, String subject, String body, @Nullable String htmlBody, String reason) {
        return new EmailNotification(
                customerId, type, null, subject, body, htmlBody, EmailNotificationStatus.FAILED, null, reason);
    }

    public void markSent(Instant now) {
        requirePending();
        attempts++;
        status = EmailNotificationStatus.SENT;
        sentAt = now;
        nextAttemptAt = null;
        lastError = null;
    }

    public void registerFailure(String error, Instant now, int maxAttempts, Duration retryDelay) {
        requirePending();
        attempts++;
        lastError = error;
        if (attempts >= maxAttempts) {
            status = EmailNotificationStatus.FAILED;
            nextAttemptAt = null;
        } else {
            nextAttemptAt = now.plus(retryDelay.multipliedBy(attempts));
        }
    }

    private void requirePending() {
        if (status != EmailNotificationStatus.PENDING) {
            throw new IllegalStateException("Email notification " + getId() + " is not pending, it is " + status);
        }
    }
}
