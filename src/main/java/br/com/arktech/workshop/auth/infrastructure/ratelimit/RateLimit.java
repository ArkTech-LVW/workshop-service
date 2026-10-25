package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;
import br.com.arktech.workshop.shared.domain.exception.TooManyRequestsException;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.time.Duration;
import java.util.Objects;

final class RateLimit {

    private static final long MAX_KEYS = 100_000;

    private final Bandwidth bandwidth;
    private final Cache<String, Bucket> buckets;

    RateLimit(RateLimitProperties.Limit limit) {
        this.bandwidth = Bandwidth.builder()
                .capacity(limit.capacity())
                .refillGreedy(limit.capacity(), limit.period())
                .build();
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(limit.period())
                .maximumSize(MAX_KEYS)
                .build();
    }

    void consume(String key) {
        Bucket bucket = Objects.requireNonNull(buckets.get(key, k -> Bucket.builder().addLimit(bandwidth).build()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }
}
