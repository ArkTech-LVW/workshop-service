package br.com.arktech.workshop.auth.domain.repository;

import br.com.arktech.workshop.auth.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
