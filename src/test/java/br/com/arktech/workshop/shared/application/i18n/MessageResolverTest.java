package br.com.arktech.workshop.shared.application.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// Loads only the i18n beans and application.yml placeholders, not the whole application, so no database is needed
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
    void shouldFailOnUnknownKey() {
        assertThatThrownBy(() -> messageResolver.get("unknown.key")).isInstanceOf(NoSuchMessageException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(PropertyPlaceholderAutoConfiguration.class)
    @ComponentScan({"br.com.arktech.workshop.shared.application.i18n", "br.com.arktech.workshop.shared.infrastructure.i18n"})
    static class I18nOnly {}
}
