package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;
import br.com.arktech.workshop.metrics.application.port.StatusTime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class GetServiceOrderTimeMetricsIntegrationTest {

    @Autowired
    private GetServiceOrderTimeMetricsUseCase useCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ServiceOrderHistoryFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ServiceOrderHistoryFixture(jdbcTemplate);
    }

    @Test
    void shouldAverageTheExecutionTimeOfTheServiceOrdersCompletedInThePeriod() {
        LocalDate day = LocalDate.of(2001, 3, 1);
        executedFor(day, ofHours(2));
        executedFor(day, ofHours(4));
        executedFor(day, ofHours(6));

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(3);
        assertThat(result.averageExecutionTime()).isEqualTo(ofHours(4));
    }

    @Test
    void shouldAverageTheExecutionTimeOfTheServiceOrdersThatIncludedEachService() {
        LocalDate day = LocalDate.of(2001, 3, 8);
        Long oilChange = fixture.serviceOffering("Troca de óleo", 60);
        Long alignment = fixture.serviceOffering("Alinhamento", 45);
        executed(fixture.openedAt(at(day, 7)).withService(oilChange, 1), ofHours(2));
        executed(fixture.openedAt(at(day, 7)).withService(oilChange, 2).withService(alignment, 1), ofHours(4));
        executed(fixture.openedAt(at(day, 7)).withService(alignment, 1), ofHours(6));
        executed(fixture.openedAt(at(day, 7)), ofHours(1));
        fixture.openedAt(at(day, 7)).withService(oilChange, 1).then("CANCELED", ofHours(1)).save();
        executed(fixture.openedAt(at(day.plusDays(1), 7)).withService(oilChange, 1), ofHours(8));

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(4);
        assertThat(result.averageExecutionTime()).isEqualTo(ofMinutes(195));
        assertThat(result.byService())
                .containsExactly(
                        new ServiceExecutionTime(alignment, "Alinhamento", 45, ofHours(5), 2),
                        new ServiceExecutionTime(oilChange, "Troca de óleo", 60, ofHours(3), 2));
    }

    @Test
    void shouldSumTheExecutionPeriodsInterruptedByASupplementaryQuote() {
        LocalDate day = LocalDate.of(2001, 3, 2);
        Long brakes = fixture.serviceOffering("Revisão de freios", 90);
        fixture.openedAt(at(day, 7))
                .withService(brakes, 1)
                .then("IN_DIAGNOSIS", ofMinutes(10))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_APPROVED", ofMinutes(30))
                .then("IN_PROGRESS", ofMinutes(15))
                .then("AWAITING_SUPPLEMENTARY_APPROVAL", ofHours(2))
                .then("SUPPLEMENTARY_APPROVED", ofHours(5))
                .then("IN_PROGRESS", ofMinutes(45))
                .then("COMPLETED", ofHours(1))
                .save();

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(1);
        assertThat(result.averageExecutionTime()).isEqualTo(ofHours(3));
        assertThat(result.byService())
                .containsExactly(new ServiceExecutionTime(brakes, "Revisão de freios", 90, ofHours(3), 1));
        assertThat(result.timeByStatus())
                .containsExactly(
                        new StatusTime("RECEIVED", ofMinutes(10), 1),
                        new StatusTime("IN_DIAGNOSIS", ofMinutes(20), 1),
                        new StatusTime("AWAITING_APPROVAL", ofMinutes(30), 1),
                        new StatusTime("QUOTE_APPROVED", ofMinutes(15), 1),
                        new StatusTime("IN_PROGRESS", ofHours(3), 1),
                        new StatusTime("AWAITING_SUPPLEMENTARY_APPROVAL", ofHours(5), 1),
                        new StatusTime("SUPPLEMENTARY_APPROVED", ofMinutes(45), 1));
    }

    @Test
    void shouldCountOnlyServiceOrdersThatWereExecutedAndCompleted() {
        LocalDate day = LocalDate.of(2001, 3, 3);
        fixture.openedAt(at(day, 8))
                .then("IN_DIAGNOSIS", ofMinutes(10))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_REJECTED", ofHours(1))
                .then("IN_DIAGNOSIS", ofHours(2))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_APPROVED", ofMinutes(30))
                .then("IN_PROGRESS", ofMinutes(15))
                .then("COMPLETED", ofHours(1))
                .save();
        fixture.openedAt(at(day, 8))
                .then("IN_DIAGNOSIS", ofMinutes(10))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_REJECTED", ofHours(1))
                .save();
        fixture.openedAt(at(day, 8))
                .then("IN_DIAGNOSIS", ofMinutes(10))
                .then("CANCELED", ofMinutes(20))
                .save();
        fixture.openedAt(at(day, 8))
                .then("IN_DIAGNOSIS", ofMinutes(10))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_APPROVED", ofMinutes(30))
                .then("IN_PROGRESS", ofMinutes(15))
                .save();

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(1);
        assertThat(result.averageExecutionTime()).isEqualTo(ofHours(1));
        assertThat(result.timeByStatus())
                .contains(new StatusTime("IN_DIAGNOSIS", ofMinutes(40), 1))
                .contains(new StatusTime("QUOTE_REJECTED", ofHours(2), 1));
    }

    @Test
    void shouldLeaveOutServiceOrdersCompletedOutsideThePeriodInTheWorkshopTimeZone() {
        LocalDate day = LocalDate.of(2001, 3, 4);
        fixture.openedAt(at(day, 18))
                .then("IN_PROGRESS", ofHours(1))
                .thenAt("COMPLETED", OffsetDateTime.parse("2001-03-04T23:30:00-03:00").toInstant())
                .save();
        fixture.openedAt(at(day, 18))
                .then("IN_PROGRESS", ofHours(1))
                .thenAt("COMPLETED", OffsetDateTime.parse("2001-03-05T00:00:00-03:00").toInstant())
                .save();
        fixture.openedAt(at(day.minusDays(1), 18))
                .then("IN_PROGRESS", ofHours(1))
                .thenAt("COMPLETED", OffsetDateTime.parse("2001-03-03T23:59:59-03:00").toInstant())
                .save();

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(1);
        assertThat(result.averageExecutionTime()).isEqualTo(ofMinutes(270));
    }

    @Test
    void shouldIgnoreTheTimeInTheCurrentStatus() {
        LocalDate day = LocalDate.of(2001, 3, 5);
        fixture.openedAt(at(day, 8))
                .then("IN_PROGRESS", ofHours(1))
                .then("COMPLETED", ofHours(2))
                .then("DELIVERED", ofHours(3))
                .save();
        fixture.openedAt(at(day, 8))
                .then("IN_PROGRESS", ofHours(1))
                .then("COMPLETED", ofHours(4))
                .save();

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isEqualTo(2);
        assertThat(result.timeByStatus())
                .containsExactly(
                        new StatusTime("RECEIVED", ofHours(1), 2),
                        new StatusTime("IN_PROGRESS", ofHours(3), 2),
                        new StatusTime("COMPLETED", ofHours(3), 1));
    }

    @Test
    void shouldRoundTheAverageToWholeSeconds() {
        LocalDate day = LocalDate.of(2001, 3, 6);
        executedFor(day, ofSeconds(1));
        executedFor(day, ofSeconds(2));

        assertThat(metricsOf(day, day).averageExecutionTime()).isEqualTo(ofSeconds(2));
    }

    @Test
    void shouldReturnNoAverageForAPeriodWithoutCompletedServiceOrders() {
        LocalDate day = LocalDate.of(2001, 3, 7);

        ServiceOrderTimeMetricsResult result = metricsOf(day, day);

        assertThat(result.serviceOrderCount()).isZero();
        assertThat(result.averageExecutionTime()).isNull();
        assertThat(result.byService()).isEmpty();
        assertThat(result.timeByStatus()).isEmpty();
    }

    private void executedFor(LocalDate day, Duration executionTime) {
        executed(fixture.openedAt(at(day, 7)), executionTime);
    }

    private void executed(ServiceOrderHistoryFixture.History history, Duration executionTime) {
        history.then("IN_DIAGNOSIS", ofMinutes(10))
                .then("AWAITING_APPROVAL", ofMinutes(20))
                .then("QUOTE_APPROVED", ofMinutes(30))
                .then("IN_PROGRESS", ofMinutes(15))
                .then("COMPLETED", executionTime)
                .then("DELIVERED", ofHours(1))
                .save();
    }

    private ServiceOrderTimeMetricsResult metricsOf(LocalDate from, LocalDate to) {
        return useCase.execute(new GetServiceOrderTimeMetricsQuery(from, to));
    }

    private static Instant at(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
    }
}
