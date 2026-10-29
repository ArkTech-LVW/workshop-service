package br.com.arktech.workshop.shared.application.security;

import java.util.Optional;

public interface CurrentUser {

    Long id();

    /** Empty when no user is logged in, as when the customer approves the quote through their own API. */
    default Optional<Long> findId() {
        return Optional.of(id());
    }
}
