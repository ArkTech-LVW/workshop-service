package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

record RouteRateLimit(@Nullable String method, PathPattern pattern, RateLimit rateLimit) {

    static final Comparator<RouteRateLimit> MOST_SPECIFIC_FIRST = Comparator
            .comparing(RouteRateLimit::pattern, PathPattern.SPECIFICITY_COMPARATOR)
            .thenComparing(route -> route.method() == null);

    static RouteRateLimit of(RateLimitProperties.RouteLimit route) {
        return new RouteRateLimit(
                route.method(), PathPatternParser.defaultInstance.parse(route.path()), new RateLimit(route.limit()));
    }

    boolean matches(String requestMethod, PathContainer path) {
        return (method == null || method.equalsIgnoreCase(requestMethod)) && pattern.matches(path);
    }
}
