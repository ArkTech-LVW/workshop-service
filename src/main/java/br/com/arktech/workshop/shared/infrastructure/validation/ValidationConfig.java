package br.com.arktech.workshop.shared.infrastructure.validation;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocaleContextMessageInterpolator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
class ValidationConfig {

    @Bean
    LocalValidatorFactoryBean validator(MessageResolver messageResolver) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setMessageInterpolator(new LocaleContextMessageInterpolator(
                new StrictMessageInterpolator(messageResolver, new ResourceBundleMessageInterpolator())));
        return factory;
    }
}
