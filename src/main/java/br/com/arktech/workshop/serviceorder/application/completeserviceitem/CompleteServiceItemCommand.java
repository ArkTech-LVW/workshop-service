package br.com.arktech.workshop.serviceorder.application.completeserviceitem;

public record CompleteServiceItemCommand(Long serviceOrderId, Long itemId) {
}
