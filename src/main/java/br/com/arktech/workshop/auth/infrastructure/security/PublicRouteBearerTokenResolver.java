package br.com.arktech.workshop.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

@RequiredArgsConstructor
public class PublicRouteBearerTokenResolver implements BearerTokenResolver {

    private final RequestMatcher publicPaths;
    private final EndpointAuthorizationManager endpointAuthorizationManager;
    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public @Nullable String resolve(HttpServletRequest request) {
        if (publicPaths.matches(request) || endpointAuthorizationManager.isPublicEndpoint(request)) {
            return null;
        }
        return delegate.resolve(request);
    }
}
