package br.com.arktech.workshop.auth.infrastructure.persistence;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {

    Sort PAGE_ORDER = Sort.by("name", "id");

    @Override
    default Page<User> findPage(Pagination pagination) {
        var page = findAll(PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }
}
