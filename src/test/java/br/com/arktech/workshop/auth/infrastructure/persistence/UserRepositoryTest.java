package br.com.arktech.workshop.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.security.Role;

import jakarta.persistence.EntityManager;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistUserWithRolesAndTimestamps() {
        User saved = userRepository.save(newUser("ana@workshop.com", Role.ADMIN, Role.MECHANIC));
        flushAndClear();

        User found = userRepository.findByEmail(new Email("ana@workshop.com")).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Ana");
        assertThat(found.getEmail()).isEqualTo(new Email("ana@workshop.com"));
        assertThat(userRepository.findByEmail(new Email(" ANA@Workshop.com"))).contains(found);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.MECHANIC);
        assertThat(found.authenticate("secret", passwordHasher)).isTrue();
        assertThat(found.getDateCreated()).isNotNull();
        assertThat(found.getLastUpdated()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(userRepository.existsByEmail(new Email("ana@workshop.com"))).isTrue();
        assertThat(userRepository.existsByEmail(new Email("other@workshop.com"))).isFalse();
    }

    @Test
    void shouldIncrementVersionAndLastUpdatedOnChange() {
        userRepository.save(newUser("bia@workshop.com", Role.ATTENDANT));
        flushAndClear();

        User user = userRepository.findByEmail(new Email("bia@workshop.com")).orElseThrow();
        user.deactivate();
        flushAndClear();

        User updated = userRepository.findByEmail(new Email("bia@workshop.com")).orElseThrow();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getLastUpdated()).isAfter(updated.getDateCreated());
    }

    // Other integration tests commit users too, so this one only looks at the last pages, where its names sort.
    @Test
    void shouldPageUsersOrderedByName() {
        userRepository.save(newNamedUser("zzz-c", "page-c@workshop.com"));
        userRepository.save(newNamedUser("zzz-a", "page-a@workshop.com"));
        userRepository.save(newNamedUser("zzz-b", "page-b@workshop.com"));
        flushAndClear();
        int total = (int) userRepository.findPage(new Pagination(0, 1)).totalElements();

        Page<User> last = userRepository.findPage(new Pagination(total - 1, 1));

        assertThat(last.totalPages()).isEqualTo(total);
        assertThat(nameAt(total - 3)).isEqualTo("zzz-a");
        assertThat(nameAt(total - 2)).isEqualTo("zzz-b");
        assertThat(last.content()).extracting(User::getName).containsExactly("zzz-c");
    }

    @Test
    void shouldReportUnknownUser() {
        assertThatThrownBy(() -> userRepository.findRequiredById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("user.notFound");
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        userRepository.save(newUser("caio@workshop.com", Role.MECHANIC));
        flushAndClear();

        assertThatThrownBy(() -> {
            userRepository.save(newUser("CAIO@workshop.com", Role.ADMIN));
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User newUser(String email, Role... roles) {
        return new User("Ana", new Email(email), "secret", Set.of(roles), passwordHasher);
    }

    private User newNamedUser(String name, String email) {
        return new User(name, new Email(email), "secret", Set.of(Role.MECHANIC), passwordHasher);
    }

    private String nameAt(int index) {
        return userRepository.findPage(new Pagination(index, 1)).content().getFirst().getName();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
