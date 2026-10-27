package br.com.arktech.workshop.auth.infrastructure.security;

import br.com.arktech.workshop.auth.infrastructure.config.JwtConfig;
import br.com.arktech.workshop.shared.api.security.EndpointAccess;
import br.com.arktech.workshop.shared.domain.security.Role;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@RequiredArgsConstructor
public class EndpointAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
    private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);
    private static final AuthorizationManager<RequestAuthorizationContext> PERMIT_ALL = (authentication, context) -> GRANTED;
    private static final AuthorizationManager<RequestAuthorizationContext> DENY_ALL = (authentication, context) -> DENIED;

    @Qualifier("requestMappingHandlerMapping")
    private final RequestMappingHandlerMapping handlerMapping;

    private final Map<Method, AuthorizationManager<RequestAuthorizationContext>> managers = new ConcurrentHashMap<>();
    private final Map<Method, EndpointAccess> accesses = new ConcurrentHashMap<>();

    public boolean isPublicEndpoint(HttpServletRequest request) {
        HandlerExecutionChain chain;
        try {
            chain = handlerMapping.getHandler(new LookupRequest(request));
        } catch (Exception ex) {
            return false;
        }
        return chain != null
                && chain.getHandler() instanceof HandlerMethod handlerMethod
                && accesses.computeIfAbsent(handlerMethod.getMethod(), method -> EndpointAccess.of(handlerMethod))
                        instanceof EndpointAccess.Public;
    }

    @Override
    public @Nullable AuthorizationResult authorize(
            Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        HandlerExecutionChain chain;
        try {
            chain = handlerMapping.getHandler(new LookupRequest(context.getRequest()));
        } catch (ServletException ex) {
            // The route exists but the request does not fit it (405, 415...). No handler will run, and the
            // DispatcherServlet answers with the same error.
            return GRANTED;
        } catch (Exception ex) {
            return DENIED;
        }
        if (chain == null || !(chain.getHandler() instanceof HandlerMethod handlerMethod)) {
            return DENIED;
        }

        return managers.computeIfAbsent(handlerMethod.getMethod(), method -> managerFor(handlerMethod))
                .authorize(authentication, context);
    }

    private static AuthorizationManager<RequestAuthorizationContext> managerFor(HandlerMethod handlerMethod) {
        return switch (EndpointAccess.of(handlerMethod)) {
            case EndpointAccess.Public publicAccess -> PERMIT_ALL;
            case EndpointAccess.AnyUser anyUser -> AuthenticatedAuthorizationManager.authenticated();
            case EndpointAccess.Roles(var roles) -> AuthorityAuthorizationManager.hasAnyRole(
                    JwtConfig.ROLE_PREFIX, roles.stream().map(Role::name).toArray(String[]::new));
            case EndpointAccess.Undeclared undeclared -> DENY_ALL;
        };
    }

    // Keeps the attributes the handler lookup writes (parsed path, matched pattern...) off the real request,
    // which the DispatcherServlet still has to route.
    private static final class LookupRequest extends HttpServletRequestWrapper {

        private final Map<String, @Nullable Object> attributes = new HashMap<>();

        LookupRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public @Nullable Object getAttribute(String name) {
            return attributes.containsKey(name) ? attributes.get(name) : super.getAttribute(name);
        }

        @Override
        public void setAttribute(String name, @Nullable Object value) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            attributes.put(name, null);
        }
    }
}
