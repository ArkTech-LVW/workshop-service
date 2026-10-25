package br.com.arktech.workshop.auth.domain.repository;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.shared.domain.email.Email;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();

    @Override
    public User save(User user) {
        ReflectionTestUtils.setField(user, "id", (long) users.size() + 1);
        users.add(user);
        return user;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    public List<User> findAll() {
        return List.copyOf(users);
    }
}
