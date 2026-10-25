package br.com.arktech.workshop.auth.domain.repository;

import br.com.arktech.workshop.auth.domain.model.User;

import br.com.arktech.workshop.shared.domain.email.Email;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
