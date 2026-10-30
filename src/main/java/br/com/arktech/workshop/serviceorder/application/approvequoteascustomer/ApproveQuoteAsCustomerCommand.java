package br.com.arktech.workshop.serviceorder.application.approvequoteascustomer;

import java.util.UUID;

public record ApproveQuoteAsCustomerCommand(UUID publicId, String customerDocument) {
}
