package br.com.arktech.workshop.auth.infrastructure.config;

import java.time.Duration;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.rate-limit")
public record RateLimitProperties(Limit general, Limit loginPerEmailAndIp, Map<String, RouteLimit> routes) {

    public RateLimitProperties(Limit general, Limit loginPerEmailAndIp, @Nullable Map<String, RouteLimit> routes) {
        this.general = general;
        this.loginPerEmailAndIp = loginPerEmailAndIp;
        this.routes = routes != null ? Map.copyOf(routes) : Map.of();
    }

    public record Limit(long capacity, Duration period) {}

    public record RouteLimit(@Nullable String method, String path, long capacity, Duration period) {

        public Limit limit() {
            return new Limit(capacity, period);
        }
    }
}
