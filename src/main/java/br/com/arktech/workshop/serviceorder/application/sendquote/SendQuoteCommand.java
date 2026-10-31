package br.com.arktech.workshop.serviceorder.application.sendquote;

import org.jspecify.annotations.Nullable;

public record SendQuoteCommand(Long serviceOrderId, @Nullable Boolean canProceedWithoutChanges) {
}
