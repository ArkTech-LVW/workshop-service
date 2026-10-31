package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.GetServiceOrderTimeMetricsQuery;
import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.GetServiceOrderTimeMetricsUseCase;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class MetricsController implements IMetricsController {

    private final GetServiceOrderTimeMetricsUseCase getServiceOrderTimeMetricsUseCase;

    @Override
    public AverageExecutionTimeResponse averageExecutionTime(LocalDate from, LocalDate to) {
        return AverageExecutionTimeResponse.from(
                getServiceOrderTimeMetricsUseCase.execute(new GetServiceOrderTimeMetricsQuery(from, to)));
    }
}
