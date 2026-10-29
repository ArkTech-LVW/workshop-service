package br.com.arktech.workshop.serviceorder.application.cancelserviceorder;

public record CancelServiceOrderCommand(Long serviceOrderId, String reason) {
}
