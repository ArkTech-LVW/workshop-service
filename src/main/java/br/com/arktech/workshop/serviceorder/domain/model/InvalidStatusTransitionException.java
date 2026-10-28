package br.com.arktech.workshop.serviceorder.domain.model;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import lombok.Getter;

@Getter
public class InvalidStatusTransitionException extends BusinessRuleException {

    private final ServiceOrderStatus from;

    private final ServiceOrderStatus to;

    public InvalidStatusTransitionException(ServiceOrderStatus from, ServiceOrderStatus to) {
        super("serviceOrder.invalidStatusTransition");
        this.from = from;
        this.to = to;
    }
}
