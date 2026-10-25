package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;
import br.com.arktech.workshop.shared.domain.exception.TooManyRequestsException;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimitTest {

    private final RateLimit rateLimit = new RateLimit(new RateLimitProperties.Limit(3, Duration.ofMinutes(1)));

    @Test
    void shouldRejectOverCapacityWithTimeUntilNextToken() {
        consume("10.0.0.1", 3);

        assertThatThrownBy(() -> rateLimit.consume("10.0.0.1"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, ex -> assertThat(ex.getRetryAfterSeconds())
                        .isBetween(1L, 20L))
                .hasMessage("request.tooManyRequests");
    }

    @Test
    void shouldCountEachKeyOnItsOwn() {
        consume("10.0.0.1", 3);

        consume("10.0.0.2", 3);
    }

    private void consume(String key, int times) {
        for (int i = 0; i < times; i++) {
            rateLimit.consume(key);
        }
    }
}
