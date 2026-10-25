package br.com.arktech.workshop.shared.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void shouldExposeCodeAndArgs() {
        DomainException exception = new NotFoundException("customer.notFound", 42L, "abc");

        assertThat(exception.getCode()).isEqualTo("customer.notFound");
        assertThat(exception.getArgs()).containsExactly(42L, "abc");
    }

    @Test
    void shouldUseCodeAsMessage() {
        assertThat(new ConflictException("customer.duplicatedDocument").getMessage())
                .isEqualTo("customer.duplicatedDocument");
    }

    @Test
    void shouldRoundRetryAfterUpToWholeSeconds() {
        assertThat(new TooManyRequestsException(Duration.ofMillis(11_001)).getRetryAfterSeconds()).isEqualTo(12);
        assertThat(new TooManyRequestsException(Duration.ofSeconds(12)).getRetryAfterSeconds()).isEqualTo(12);
        assertThat(new TooManyRequestsException(Duration.ZERO).getRetryAfterSeconds()).isEqualTo(1);
    }

    @Test
    void shouldCopyArgsDefensively() {
        Object[] args = {"original"};
        DomainException exception = new BusinessRuleException("serviceOrder.invalidStatus", args);

        args[0] = "changed by caller";
        exception.getArgs()[0] = "changed by reader";

        assertThat(exception.getArgs()).containsExactly("original");
    }
}
