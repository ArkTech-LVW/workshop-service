package br.com.arktech.workshop.notification.application.sendpendingemails;

import br.com.arktech.workshop.notification.domain.model.EmailNotification;
import br.com.arktech.workshop.notification.domain.port.EmailSender;
import br.com.arktech.workshop.notification.domain.repository.EmailNotificationRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendPendingEmailsUseCase implements VoidUseCase<SendPendingEmailsCommand> {

    private final EmailNotificationRepository emailNotificationRepository;
    private final EmailSender emailSender;

    @Override
    @Transactional
    public void execute(SendPendingEmailsCommand command) {
        for (EmailNotification email : emailNotificationRepository.lockPendingDueAt(command.now(), command.limit())) {
            try {
                emailSender.send(Objects.requireNonNull(email.getRecipient()), email.getSubject(), email.getBody());
                email.markSent(command.now());
            } catch (RuntimeException ex) {
                log.warn("Email notification failed [id={}, attempt={}]", email.getId(), email.getAttempts() + 1, ex);
                email.registerFailure(
                        String.valueOf(ex.getMessage()), command.now(), command.maxAttempts(), command.retryDelay());
            }
            emailNotificationRepository.save(email);
        }
    }
}
