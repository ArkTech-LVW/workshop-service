package br.com.arktech.workshop.shared.application.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = MessageResolverTest.I18nOnly.class)
class MessageResolverTest {

    @Autowired
    private MessageResolver messageResolver;

    @Test
    void shouldResolveMessageWithoutArgs() {
        assertThat(messageResolver.get("test.message"))
                .isEqualTo("Mensagem de teste para validação da configuração.");
    }

    @Test
    void shouldResolveMessageWithArgs() {
        assertThat(messageResolver.get("test.messageWithArgs", "o resolver", 2))
                .isEqualTo("Mensagem de teste para o resolver com 2 argumentos.");
    }

    @Test
    void shouldIgnoreTheRequestLocale() {
        LocaleContextHolder.setLocale(Locale.US);
        try {
            assertThat(messageResolver.get("test.message"))
                    .isEqualTo("Mensagem de teste para validação da configuração.");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void shouldFailOnUnknownKey() {
        assertThatThrownBy(() -> messageResolver.get("unknown.key")).isInstanceOf(NoSuchMessageException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(PropertyPlaceholderAutoConfiguration.class)
    @ComponentScan({"br.com.arktech.workshop.shared.application.i18n", "br.com.arktech.workshop.shared.infrastructure.i18n"})
    static class I18nOnly {}
}
