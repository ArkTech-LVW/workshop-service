package br.com.arktech.workshop.serviceorder.application.rejectquote;

public record RejectQuoteCommand(Long serviceOrderId, String reason) {
}
