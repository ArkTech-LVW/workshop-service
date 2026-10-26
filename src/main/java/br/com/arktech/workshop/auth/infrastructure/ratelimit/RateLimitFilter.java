package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;
import br.com.arktech.workshop.shared.domain.exception.TooManyRequestsException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final RequestMatcher HEALTH =
            PathPatternRequestMatcher.withDefaults().matcher("/actuator/health/**");

    private final RateLimit general;
    private final List<RouteRateLimit> routes;
    private final HandlerExceptionResolver exceptionResolver;

    public RateLimitFilter(RateLimitProperties properties, HandlerExceptionResolver exceptionResolver) {
        this.general = new RateLimit(properties.general());
        this.routes = properties.routes().values().stream()
                .map(RouteRateLimit::of)
                .sorted(RouteRateLimit.MOST_SPECIFIC_FIRST)
                .toList();
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HEALTH.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            rateLimitOf(request).consume(clientOf(request));
        } catch (TooManyRequestsException ex) {
            exceptionResolver.resolveException(request, response, null, ex);
            return;
        }

        chain.doFilter(request, response);
    }

    private RateLimit rateLimitOf(HttpServletRequest request) {
        PathContainer path = PathContainer.parsePath(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
        return routes.stream()
                .filter(route -> route.matches(request.getMethod(), path))
                .findFirst()
                .map(RouteRateLimit::rateLimit)
                .orElse(general);
    }

    private static String clientOf(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token
                ? "user:" + token.getName()
                : "ip:" + request.getRemoteAddr();
    }
}
