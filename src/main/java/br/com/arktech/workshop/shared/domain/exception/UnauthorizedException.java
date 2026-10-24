package br.com.arktech.workshop.shared.domain.exception;

public non-sealed class UnauthorizedException extends DomainException {

    public UnauthorizedException(String code, Object... args) {
        super(code, args);
    }
}
