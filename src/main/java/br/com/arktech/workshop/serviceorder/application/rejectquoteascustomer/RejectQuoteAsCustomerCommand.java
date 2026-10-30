package br.com.arktech.workshop.serviceorder.application.rejectquoteascustomer;

import java.util.UUID;

public record RejectQuoteAsCustomerCommand(UUID publicId, String customerDocument, String reason) {
}
