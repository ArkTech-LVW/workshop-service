package br.com.arktech.workshop.shared.api.error;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(ApiErrors.class)
@ComponentScan({
    "br.com.arktech.workshop.shared.application.i18n",
    "br.com.arktech.workshop.shared.infrastructure.i18n",
    "br.com.arktech.workshop.shared.infrastructure.validation"
})
class ApiErrorTestConfig {}
