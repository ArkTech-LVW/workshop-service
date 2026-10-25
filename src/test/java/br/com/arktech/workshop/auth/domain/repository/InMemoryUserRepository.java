package br.com.arktech.workshop.auth.domain.repository;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();

    @Override
    public User save(User user) {
        if (!users.contains(user)) {
            ReflectionTestUtils.setField(user, "id", (long) users.size() + 1);
            users.add(user);
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public Page<User> findPage(Pagination pagination) {
        List<User> content = users.stream()
                .sorted(Comparator.comparing(User::getName).thenComparing(User::getId))
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .toList();
        return new Page<>(content, pagination, users.size());
    }

    public List<User> findAll() {
        return List.copyOf(users);
    }
}
