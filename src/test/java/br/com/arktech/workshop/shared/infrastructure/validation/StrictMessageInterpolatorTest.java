package br.com.arktech.workshop.shared.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.StaticMessageSource;

class StrictMessageInterpolatorTest {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("jakarta.validation.constraints.NotBlank.message", PT_BR, "Não pode estar em branco.");
        messageSource.addMessage("test.size", PT_BR, "Deve ter entre {min} e {max} caracteres.");

        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new StrictMessageInterpolator(
                        new MessageResolver(messageSource, PT_BR), new ResourceBundleMessageInterpolator()))
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldResolveKey() {
        assertThat(messageOf(new DefaultMessage(""))).isEqualTo("Não pode estar em branco.");
    }

    @Test
    void shouldInterpolateConstraintAttributes() {
        assertThat(messageOf(new SizeMessage("a"))).isEqualTo("Deve ter entre 3 e 10 caracteres.");
    }

    @Test
    void shouldRejectLiteralText() {
        assertThatThrownBy(() -> validator.validate(new LiteralMessage("")))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validation message must be a {key}: literal text");
    }

    @Test
    void shouldRejectUnknownKey() {
        assertThatThrownBy(() -> validator.validate(new MissingKeyMessage("")))
                .rootCause()
                .isInstanceOf(NoSuchMessageException.class)
                .hasMessageContaining("test.missingKey");
    }

    private String messageOf(Object bean) {
        return validator.validate(bean).stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow();
    }

    private record DefaultMessage(@NotBlank String value) {}

    private record SizeMessage(@Size(min = 3, max = 10, message = "{test.size}") String value) {}

    private record LiteralMessage(@NotBlank(message = "literal text") String value) {}

    private record MissingKeyMessage(@NotBlank(message = "{test.missingKey}") String value) {}
}
