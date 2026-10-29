package br.com.arktech.workshop.serviceorder.application.removeserviceitem;

public record RemoveServiceItemCommand(Long serviceOrderId, Long itemId) {
}
