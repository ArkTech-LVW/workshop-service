package br.com.arktech.workshop.serviceorder.application.removepartitem;

public record RemovePartItemCommand(Long serviceOrderId, Long itemId) {
}
