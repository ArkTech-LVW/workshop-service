package br.com.arktech.workshop.shared.domain.exception;

import lombok.Getter;

public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ConflictException, BusinessRuleException {

    @Getter
    private final String code;

    private final Object[] args;

    protected DomainException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args.clone();
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
