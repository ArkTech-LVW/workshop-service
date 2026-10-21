package br.com.arktech.workshop.shared.application.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.NoSuchMessageException;

@SpringBootTest
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
}
