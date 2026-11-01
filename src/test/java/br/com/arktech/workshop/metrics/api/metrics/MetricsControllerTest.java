package br.com.arktech.workshop.metrics.api.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.GetServiceOrderTimeMetricsQuery;
import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.GetServiceOrderTimeMetricsUseCase;
import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.ServiceOrderTimeMetricsResult;
import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;
import br.com.arktech.workshop.metrics.application.port.StatusTime;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class MetricsControllerTest {

    private static final String ROUTE = "/admin/metrics/averageExecutionTime";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetServiceOrderTimeMetricsUseCase getServiceOrderTimeMetricsUseCase;

    @Test
    void shouldReturnTheMetricsInMinutesAndHoursAndMinutes() throws Exception {
        when(getServiceOrderTimeMetricsUseCase.execute(any())).thenReturn(new ServiceOrderTimeMetricsResult(
                2,
                Duration.ofMinutes(150),
                List.of(new ServiceExecutionTime(3L, "Troca de óleo", 60, Duration.ofSeconds(90), 2, 3)),
                List.of(new StatusTime("AWAITING_APPROVAL", Duration.ofHours(27).plusMinutes(5), 1))));

        mockMvc.perform(get(ROUTE).param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                        {"serviceOrderCount": 2,
                         "averageExecutionTime": {"minutes": 150, "formatted": "02:30"},
                         "byService": [{"serviceOfferingId": 3, "name": "Troca de óleo", "estimatedMinutes": 60,
                                        "serviceOrderCount": 2, "executionCount": 3,
                                        "averageExecutionTime": {"minutes": 2, "formatted": "00:02"}}],
                         "timeByStatus": [{"status": "AWAITING_APPROVAL", "serviceOrderCount": 1,
                                           "averageTime": {"minutes": 1625, "formatted": "27:05"}}]}
                        """,
                        JsonCompareMode.STRICT));
        verify(getServiceOrderTimeMetricsUseCase)
                .execute(new GetServiceOrderTimeMetricsQuery(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void shouldReturnANullAverageWhenThereIsNoServiceOrderInThePeriod() throws Exception {
        when(getServiceOrderTimeMetricsUseCase.execute(any()))
                .thenReturn(new ServiceOrderTimeMetricsResult(0, null, List.of(), List.of()));

        mockMvc.perform(get(ROUTE).param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                        {"serviceOrderCount": 0, "averageExecutionTime": null, "byService": [], "timeByStatus": []}
                        """,
                        JsonCompareMode.STRICT));
    }

    @Test
    void shouldRequireBothDates() throws Exception {
        mockMvc.perform(get(ROUTE).param("from", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalidFields"))
                .andExpect(jsonPath("$.details[0].field").value("to"));
        verifyNoInteractions(getServiceOrderTimeMetricsUseCase);
    }

    @Test
    void shouldRejectADateOutsideTheIsoFormat() throws Exception {
        mockMvc.perform(get(ROUTE).param("from", "01/10/2026").param("to", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalidFields"))
                .andExpect(jsonPath("$.details[0].field").value("from"));
        verifyNoInteractions(getServiceOrderTimeMetricsUseCase);
    }

    @Test
    void shouldAnswerUnprocessableContentForAnInvalidPeriod() throws Exception {
        when(getServiceOrderTimeMetricsUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("metrics.invalidPeriod", "2026-10-31", "2026-10-01"));

        mockMvc.perform(get(ROUTE).param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(
                        """
                        {"code": "metrics.invalidPeriod",
                         "message": "O início do período (2026-10-31) não pode ser depois do fim (2026-10-01)."}
                        """,
                        JsonCompareMode.STRICT));
    }

    @Test
    void shouldAnswerUnprocessableContentForAPeriodLongerThanAllowed() throws Exception {
        when(getServiceOrderTimeMetricsUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("metrics.periodTooLong", 366));

        mockMvc.perform(get(ROUTE).param("from", "2025-01-01").param("to", "2026-10-31"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(
                        """
                        {"code": "metrics.periodTooLong", "message": "O período pode ter no máximo 366 dias."}
                        """,
                        JsonCompareMode.STRICT));
    }
}
