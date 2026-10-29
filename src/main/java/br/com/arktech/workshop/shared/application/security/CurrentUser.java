package br.com.arktech.workshop.shared.application.security;

import java.util.Optional;

public interface CurrentUser {

    Long id();

    default Optional<Long> findId() {
        return Optional.of(id());
    }
}
