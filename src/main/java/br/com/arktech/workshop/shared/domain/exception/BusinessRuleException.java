package br.com.arktech.workshop.shared.domain.exception;

public non-sealed class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, Object... args) {
        super(code, args);
    }
}
