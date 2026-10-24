package br.com.arktech.workshop.shared.domain.exception;

public non-sealed class NotFoundException extends DomainException {

    public NotFoundException(String code, Object... args) {
        super(code, args);
    }
}
