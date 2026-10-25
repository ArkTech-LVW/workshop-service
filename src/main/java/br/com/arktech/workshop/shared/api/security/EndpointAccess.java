package br.com.arktech.workshop.shared.api.security;

import br.com.arktech.workshop.shared.domain.security.Role;
import java.lang.reflect.AnnotatedElement;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

// Read from the handler method or its type, including the IXxxController interface. The method wins.
public sealed interface EndpointAccess {

    record Public() implements EndpointAccess {}

    record AnyUser() implements EndpointAccess {}

    record Roles(Set<Role> roles) implements EndpointAccess {

        public Roles {
            roles = Set.copyOf(roles);
        }
    }

    record Undeclared() implements EndpointAccess {}

    static EndpointAccess of(HandlerMethod handlerMethod) {
        // Both are read even when the method decides, so a misconfigured type is always reported.
        EndpointAccess onType = declaredOn(handlerMethod.getBeanType());
        EndpointAccess onMethod = declaredOn(handlerMethod.getMethod());
        return onMethod instanceof Undeclared ? onType : onMethod;
    }

    private static EndpointAccess declaredOn(AnnotatedElement element) {
        boolean isPublic = AnnotatedElementUtils.hasAnnotation(element, PublicEndpoint.class);
        boolean isAuthenticated = AnnotatedElementUtils.hasAnnotation(element, Authenticated.class);
        AllowedRoles allowedRoles = AnnotatedElementUtils.findMergedAnnotation(element, AllowedRoles.class);

        boolean hasRoles = allowedRoles != null;

        long declared = Stream.of(isPublic, isAuthenticated, hasRoles).filter(Boolean::booleanValue).count();
        if (declared > 1) {
            throw new IllegalStateException("More than one access annotation on " + element);
        }

        if (isPublic) {
            return new Public();
        }

        if (isAuthenticated) {
            return new AnyUser();
        }

        if (allowedRoles == null) {
            return new Undeclared();
        }

        if (allowedRoles.value().length == 0) {
            throw new IllegalStateException("@AllowedRoles without roles on " + element);
        }

        return new Roles(EnumSet.copyOf(List.of(allowedRoles.value())));
    }
}
