package br.com.arktech.workshop.serviceorder.application.addpartitem;

public record AddPartItemCommand(Long serviceOrderId, Long partId, int quantity) {
}
