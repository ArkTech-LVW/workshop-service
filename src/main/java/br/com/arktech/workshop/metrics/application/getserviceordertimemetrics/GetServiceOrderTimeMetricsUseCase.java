package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import br.com.arktech.workshop.metrics.application.port.ServiceOrderHistoryReader;
import br.com.arktech.workshop.metrics.application.port.StatusTime;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetServiceOrderTimeMetricsUseCase
        implements UseCase<GetServiceOrderTimeMetricsQuery, ServiceOrderTimeMetricsResult> {

    private static final String EXECUTION_STATUS = "IN_PROGRESS";

    private final ServiceOrderHistoryReader serviceOrderHistoryReader;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ServiceOrderTimeMetricsResult execute(GetServiceOrderTimeMetricsQuery query) {
        if (query.from().isAfter(query.to())) {
            throw new BusinessRuleException(
                    "metrics.invalidPeriod", query.from().toString(), query.to().toString());
        }
        Instant start = startOf(query.from());
        Instant end = startOf(query.to().plusDays(1));
        long serviceOrderCount = serviceOrderHistoryReader.countCompletedBetween(start, end);
        List<StatusTime> timeByStatus =
                serviceOrderHistoryReader.averageTimeByStatusOfCompletedBetween(start, end);
        return new ServiceOrderTimeMetricsResult(
                serviceOrderCount,
                timeByStatus.stream()
                        .filter(statusTime -> statusTime.status().equals(EXECUTION_STATUS))
                        .map(StatusTime::averageTime)
                        .findFirst()
                        .orElse(null),
                timeByStatus);
    }

    private Instant startOf(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
    }
}
