package br.com.arktech.workshop.shared.application.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;

public final class TestMessageResolver {

    private TestMessageResolver() {
    }

    public static MessageResolver withApplicationMessages() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        return new MessageResolver(messageSource, Locale.of("pt", "BR"));
    }
}
