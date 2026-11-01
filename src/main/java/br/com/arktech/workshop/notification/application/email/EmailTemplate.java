package br.com.arktech.workshop.notification.application.email;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EmailTemplate {

    private final MessageResolver messageResolver;

    RenderedEmail render(Notification notification) {
        String prefix = "notification.email." + keyOf(notification.type());
        Object[] parameters = notification.parameters().toArray();
        return new RenderedEmail(
                messageResolver.get(prefix + ".subject", parameters),
                messageResolver.get(prefix + ".body", parameters));
    }

    private static String keyOf(NotificationType type) {
        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        return words[0]
                + Arrays.stream(words, 1, words.length)
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                        .collect(Collectors.joining());
    }

    record RenderedEmail(String subject, String body) {
    }
}
