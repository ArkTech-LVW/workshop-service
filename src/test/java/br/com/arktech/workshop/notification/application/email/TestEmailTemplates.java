package br.com.arktech.workshop.notification.application.email;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

final class TestEmailTemplates {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private TestEmailTemplates() {
    }

    static EmailTemplate create() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages", "notifications");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(messageSource);

        return new EmailTemplate(new MessageResolver(messageSource, PT_BR), templateEngine, PT_BR);
    }
}
