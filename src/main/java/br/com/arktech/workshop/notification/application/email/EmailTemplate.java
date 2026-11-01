package br.com.arktech.workshop.notification.application.email;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Component
class EmailTemplate {

    private final MessageResolver messageResolver;
    private final ITemplateEngine templateEngine;
    private final Locale locale;

    EmailTemplate(
            MessageResolver messageResolver,
            ITemplateEngine templateEngine,
            @Value("${spring.web.locale}") Locale locale) {
        this.messageResolver = messageResolver;
        this.templateEngine = templateEngine;
        this.locale = locale;
    }

    RenderedEmail render(Notification notification) {
        String prefix = "notification.email." + camelCaseOf(notification.type());
        Object[] parameters = notification.parameters().toArray();
        String subject = messageResolver.get(prefix + ".subject", parameters);

        Context context = new Context(locale);
        context.setVariable("subject", subject);
        context.setVariable("parameters", notification.parameters());

        return new RenderedEmail(
                subject,
                messageResolver.get(prefix + ".body", parameters),
                templateEngine.process("email/" + kebabCaseOf(notification.type()), context));
    }

    private static String camelCaseOf(NotificationType type) {
        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        return words[0]
                + Arrays.stream(words, 1, words.length)
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                        .collect(Collectors.joining());
    }

    private static String kebabCaseOf(NotificationType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    record RenderedEmail(String subject, String body, String htmlBody) {
    }
}
