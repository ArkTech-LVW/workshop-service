package br.com.arktech.workshop.serviceorder.application.startserviceitem;

public record StartServiceItemCommand(Long serviceOrderId, Long itemId) {
}
