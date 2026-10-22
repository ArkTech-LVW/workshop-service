package br.com.arktech.workshop.shared.domain.exception;

public non-sealed class ConflictException extends DomainException {

    public ConflictException(String code, Object... args) {
        super(code, args);
    }
}
