package br.com.arktech.workshop.shared.application.i18n;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MoneyFormatter {

    private final Locale locale;

    public MoneyFormatter(@Value("${spring.web.locale}") Locale locale) {
        this.locale = locale;
    }

    public String format(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(locale).format(amount).replace(' ', ' ');
    }
}
