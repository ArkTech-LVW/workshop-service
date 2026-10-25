package br.com.arktech.workshop.auth.infrastructure.persistence;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(Email email);
}
