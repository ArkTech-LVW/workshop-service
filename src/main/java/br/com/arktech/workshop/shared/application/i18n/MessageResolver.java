package br.com.arktech.workshop.shared.application.i18n;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MessageResolver {

    private final MessageSource messageSource;
    private final Locale locale;

    public MessageResolver(MessageSource messageSource, @Value("${spring.web.locale}") Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    public String get(String code) {
        return messageSource.getMessage(code, null, locale);
    }

    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }
}
