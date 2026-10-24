package br.com.arktech.workshop.shared.infrastructure.validation;

import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import jakarta.validation.MessageInterpolator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class StrictMessageInterpolator implements MessageInterpolator {

    private static final Pattern MESSAGE_KEY = Pattern.compile("^\\{([^{}]+)\\}$");

    private final MessageResolver messageResolver;
    private final MessageInterpolator delegate;

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return delegate.interpolate(resolve(messageTemplate), context);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        return delegate.interpolate(resolve(messageTemplate), context, locale);
    }

    private String resolve(String messageTemplate) {
        Matcher matcher = MESSAGE_KEY.matcher(messageTemplate);
        if (!matcher.matches()) {
            throw new IllegalStateException("Validation message must be a {key}: " + messageTemplate);
        }
        return messageResolver.get(matcher.group(1));
    }
}
