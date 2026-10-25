package br.com.arktech.workshop.auth.domain.repository;

import br.com.arktech.workshop.auth.domain.model.User;

import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    Page<User> findPage(Pagination pagination);

    default User findRequiredById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException("user.notFound", id.toString()));
    }
}
