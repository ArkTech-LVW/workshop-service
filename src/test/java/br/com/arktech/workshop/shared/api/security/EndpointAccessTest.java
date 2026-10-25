package br.com.arktech.workshop.shared.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import br.com.arktech.workshop.shared.domain.security.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class EndpointAccessTest {

    @Test
    void shouldBeUndeclaredWithoutAnnotation() {
        assertThat(accessOf(new UndeclaredController(), "endpoint")).isEqualTo(new EndpointAccess.Undeclared());
    }

    @Test
    void shouldReadAnnotationsFromTheMethod() {
        assertThat(accessOf(new UndeclaredController(), "open")).isEqualTo(new EndpointAccess.Public());
        assertThat(accessOf(new UndeclaredController(), "loggedIn")).isEqualTo(new EndpointAccess.AnyUser());
        assertThat(accessOf(new UndeclaredController(), "admin"))
                .isEqualTo(new EndpointAccess.Roles(Set.of(Role.ADMIN)));
    }

    @Test
    void shouldReadAnnotationsFromTheControllerInterface() {
        assertThat(accessOf(new InterfaceController(), "endpoint"))
                .isEqualTo(new EndpointAccess.Roles(Set.of(Role.ATTENDANT)));
        assertThat(accessOf(new InterfaceController(), "open")).isEqualTo(new EndpointAccess.Public());
    }

    @Test
    void shouldLetTheMethodWinOverTheType() {
        assertThat(accessOf(new TypeController(), "endpoint")).isEqualTo(new EndpointAccess.AnyUser());
        assertThat(accessOf(new TypeController(), "open")).isEqualTo(new EndpointAccess.Public());
    }

    @Test
    void shouldReadEveryAllowedRole() {
        assertThat(accessOf(new UndeclaredController(), "frontDesk"))
                .isEqualTo(new EndpointAccess.Roles(Set.of(Role.ADMIN, Role.ATTENDANT)));
    }

    @Test
    void shouldRejectTwoAccessAnnotationsOnTheSameElement() {
        assertThatIllegalStateException()
                .isThrownBy(() -> accessOf(new MisconfiguredController(), "twice"))
                .withMessageContaining("More than one access annotation");
        assertThatIllegalStateException()
                .isThrownBy(() -> accessOf(new MisconfiguredController(), "publicWithRoles"))
                .withMessageContaining("More than one access annotation");
    }

    @Test
    void shouldRejectAllowedRolesWithoutRoles() {
        assertThatIllegalStateException()
                .isThrownBy(() -> accessOf(new MisconfiguredController(), "noRoles"))
                .withMessageContaining("@AllowedRoles without roles");
    }

    private static EndpointAccess accessOf(Object controller, String methodName) {
        try {
            return EndpointAccess.of(new HandlerMethod(controller, methodName));
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static class UndeclaredController {

        public void endpoint() {}

        @PublicEndpoint
        public void open() {}

        @Authenticated
        public void loggedIn() {}

        @AllowedRoles(Role.ADMIN)
        public void admin() {}

        @AllowedRoles({Role.ADMIN, Role.ATTENDANT})
        public void frontDesk() {}
    }

    @AllowedRoles(Role.ATTENDANT)
    interface IInterfaceController {

        void endpoint();

        @PublicEndpoint
        void open();
    }

    static class InterfaceController implements IInterfaceController {

        @Override
        public void endpoint() {}

        @Override
        public void open() {}
    }

    @Authenticated
    static class TypeController {

        public void endpoint() {}

        @PublicEndpoint
        public void open() {}
    }

    static class MisconfiguredController {

        @PublicEndpoint
        @Authenticated
        public void twice() {}

        @PublicEndpoint
        @AllowedRoles(Role.ADMIN)
        public void publicWithRoles() {}

        @AllowedRoles({})
        public void noRoles() {}
    }
}
