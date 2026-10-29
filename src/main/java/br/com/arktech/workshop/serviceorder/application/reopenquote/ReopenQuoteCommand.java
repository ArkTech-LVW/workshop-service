package br.com.arktech.workshop.serviceorder.application.reopenquote;

import org.jspecify.annotations.Nullable;

public record ReopenQuoteCommand(Long serviceOrderId, @Nullable String description) {
}
