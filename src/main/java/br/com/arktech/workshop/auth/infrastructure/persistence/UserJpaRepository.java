package br.com.arktech.workshop.auth.infrastructure.persistence;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.security.Role;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {

    Sort PAGE_ORDER = Sort.by("name", "id");

    @Override
    default Page<User> findPage(Pagination pagination) {
        var page = findAll(PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    @Override
    default long countActiveAdminsForUpdate() {
        lockActiveAdmins();
        return countActiveByRole(Role.ADMIN);
    }

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext('user.activeAdmins'))", nativeQuery = true)
    int lockActiveAdmins();

    @Query("SELECT count(u) FROM User u JOIN u.roles r WHERE u.active = true AND r.role = :role")
    long countActiveByRole(@Param("role") Role role);
}
