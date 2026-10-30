package br.com.arktech.workshop.notification.application.sendnotification;

import br.com.arktech.workshop.notification.application.port.Notifier;
import br.com.arktech.workshop.notification.domain.model.Notification;
import br.com.arktech.workshop.shared.application.VoidUseCase;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendNotificationUseCase implements VoidUseCase<SendNotificationCommand> {

    private final MessageResolver messageResolver;
    private final Notifier notifier;

    @Override
    public void execute(SendNotificationCommand command) {
        String reason = command.reason();
        Object[] messageArgs = reason == null
                ? new Object[] {command.serviceOrderPublicId(), command.totalPrice()}
                : new Object[] {command.serviceOrderPublicId(), command.totalPrice(), reason};
        String message = messageResolver.get(command.type().getMessageKey(), messageArgs);
        notifier.send(new Notification(
                command.type(),
                command.serviceOrderPublicId(),
                command.customerId(),
                command.totalPrice(),
                message));
    }
}
