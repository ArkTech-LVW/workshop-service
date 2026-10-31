package br.com.arktech.workshop.serviceorder.application.approveoriginalonlyascustomer;

import java.util.UUID;

public record ApproveOriginalOnlyAsCustomerCommand(UUID publicId, String customerDocument) {
}
