package br.com.arktech.workshop.auth.infrastructure.persistence;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
}
