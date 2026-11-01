package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;
import br.com.arktech.workshop.metrics.application.port.ServiceOrderHistoryReader;
import br.com.arktech.workshop.metrics.application.port.StatusTime;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GetServiceOrderTimeMetricsUseCaseTest {

    private static final LocalDate OCTOBER_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCTOBER_31 = LocalDate.of(2026, 10, 31);

    private final FakeServiceOrderHistoryReader reader = new FakeServiceOrderHistoryReader();
    private final GetServiceOrderTimeMetricsUseCase useCase = new GetServiceOrderTimeMetricsUseCase(
            reader, Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    @Test
    void shouldQueryFromTheStartOfTheFirstDayUntilTheStartOfTheDayAfterTheLastInTheWorkshopTimeZone() {
        useCase.execute(new GetServiceOrderTimeMetricsQuery(OCTOBER_1, OCTOBER_31));

        assertThat(reader.periods)
                .hasSize(3)
                .containsOnly(List.of(Instant.parse("2026-10-01T03:00:00Z"), Instant.parse("2026-11-01T03:00:00Z")));
    }

    @Test
    void shouldAcceptASingleDayPeriod() {
        useCase.execute(new GetServiceOrderTimeMetricsQuery(OCTOBER_31, OCTOBER_31));

        assertThat(reader.periods)
                .containsOnly(List.of(Instant.parse("2026-10-31T03:00:00Z"), Instant.parse("2026-11-01T03:00:00Z")));
    }

    @Test
    void shouldRejectAPeriodThatStartsAfterItEnds() {
        assertThatThrownBy(() -> useCase.execute(new GetServiceOrderTimeMetricsQuery(OCTOBER_31, OCTOBER_1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("metrics.invalidPeriod");
        assertThat(reader.periods).isEmpty();
    }

    @Test
    void shouldAcceptAPeriodOf366Days() {
        useCase.execute(new GetServiceOrderTimeMetricsQuery(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)));

        assertThat(reader.periods).isNotEmpty();
    }

    @Test
    void shouldRejectAPeriodLongerThan366Days() {
        assertThatThrownBy(() -> useCase.execute(
                        new GetServiceOrderTimeMetricsQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("metrics.periodTooLong");
        assertThat(reader.periods).isEmpty();
    }

    @Test
    void shouldTakeTheAverageExecutionTimeFromTheTimeInProgress() {
        reader.serviceOrderCount = 2;
        reader.timeByStatus = List.of(
                new StatusTime("IN_DIAGNOSIS", Duration.ofHours(1), 2),
                new StatusTime("IN_PROGRESS", Duration.ofHours(3), 2),
                new StatusTime("COMPLETED", Duration.ofHours(5), 1));
        reader.byService = List.of(new ServiceExecutionTime(3L, "Troca de óleo", 60, Duration.ofHours(3), 2, 2));

        ServiceOrderTimeMetricsResult result =
                useCase.execute(new GetServiceOrderTimeMetricsQuery(OCTOBER_1, OCTOBER_31));

        assertThat(result.serviceOrderCount()).isEqualTo(2);
        assertThat(result.averageExecutionTime()).isEqualTo(Duration.ofHours(3));
        assertThat(result.byService()).isEqualTo(reader.byService);
        assertThat(result.timeByStatus()).isEqualTo(reader.timeByStatus);
    }

    @Test
    void shouldReturnNoAverageWhenNoServiceOrderWasCompletedInThePeriod() {
        ServiceOrderTimeMetricsResult result =
                useCase.execute(new GetServiceOrderTimeMetricsQuery(OCTOBER_1, OCTOBER_31));

        assertThat(result.serviceOrderCount()).isZero();
        assertThat(result.averageExecutionTime()).isNull();
        assertThat(result.byService()).isEmpty();
        assertThat(result.timeByStatus()).isEmpty();
    }

    private static final class FakeServiceOrderHistoryReader implements ServiceOrderHistoryReader {

        private final List<List<Instant>> periods = new ArrayList<>();
        private long serviceOrderCount;
        private List<StatusTime> timeByStatus = List.of();
        private List<ServiceExecutionTime> byService = List.of();

        @Override
        public long countCompletedBetween(Instant start, Instant end) {
            periods.add(List.of(start, end));
            return serviceOrderCount;
        }

        @Override
        public List<StatusTime> averageTimeByStatusOfCompletedBetween(Instant start, Instant end) {
            periods.add(List.of(start, end));
            return timeByStatus;
        }

        @Override
        public List<ServiceExecutionTime> averageExecutionTimeByServiceOfCompletedBetween(Instant start, Instant end) {
            periods.add(List.of(start, end));
            return byService;
        }
    }
}
