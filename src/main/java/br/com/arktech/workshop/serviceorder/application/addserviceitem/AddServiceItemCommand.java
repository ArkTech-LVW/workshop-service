package br.com.arktech.workshop.serviceorder.application.addserviceitem;

public record AddServiceItemCommand(Long serviceOrderId, Long serviceOfferingId, int quantity) {
}
