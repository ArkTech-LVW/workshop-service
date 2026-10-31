package br.com.arktech.workshop.metrics.api.metrics;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.metrics.application.getserviceordertimemetrics.ServiceOrderHistoryFixture;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MetricsIntegrationTest {

    private static final String ROUTE = "/admin/metrics/averageExecutionTime";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ServiceOrderHistoryFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ServiceOrderHistoryFixture(jdbcTemplate);
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(get(ROUTE).param("from", "2001-04-01").param("to", "2001-04-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyUsersWithoutAdminRole() throws Exception {
        mockMvc.perform(get(ROUTE)
                        .param("from", "2001-04-01")
                        .param("to", "2001-04-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ATTENDANT, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldReturnTheMetricsOfTheServiceOrdersCompletedInThePeriod() throws Exception {
        LocalDate day = LocalDate.of(2001, 4, 2);
        Long oilChange = fixture.serviceOffering("Troca de óleo", 60);
        Long alignment = fixture.serviceOffering("Alinhamento", 45);
        fixture.openedAt(at(day.minusDays(1), 8))
                .withService(oilChange, 1)
                .then("IN_DIAGNOSIS", ofMinutes(30))
                .then("AWAITING_APPROVAL", ofMinutes(30))
                .then("QUOTE_APPROVED", ofHours(25).plusMinutes(5))
                .then("IN_PROGRESS", ofMinutes(10))
                .then("COMPLETED", ofHours(2))
                .then("DELIVERED", ofHours(1))
                .save();
        fixture.openedAt(at(day.minusDays(1), 8))
                .withService(oilChange, 1)
                .withService(alignment, 1)
                .then("IN_DIAGNOSIS", ofMinutes(30))
                .then("AWAITING_APPROVAL", ofMinutes(30))
                .then("QUOTE_APPROVED", ofMinutes(55))
                .then("IN_PROGRESS", ofMinutes(10))
                .thenAt("COMPLETED", at(day, 15))
                .save();

        whenAdminRequests("2001-04-02", "2001-04-02")
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                        {"serviceOrderCount": 2,
                         "averageExecutionTime": {"minutes": 928, "formatted": "15:28"},
                         "byService": [
                           {"serviceOfferingId": %d, "name": "Alinhamento", "estimatedMinutes": 45,
                            "serviceOrderCount": 1, "averageExecutionTime": {"minutes": 1735, "formatted": "28:55"}},
                           {"serviceOfferingId": %d, "name": "Troca de óleo", "estimatedMinutes": 60,
                            "serviceOrderCount": 2, "averageExecutionTime": {"minutes": 928, "formatted": "15:28"}}],
                         "timeByStatus": [
                           {"status": "RECEIVED", "serviceOrderCount": 2,
                            "averageTime": {"minutes": 30, "formatted": "00:30"}},
                           {"status": "IN_DIAGNOSIS", "serviceOrderCount": 2,
                            "averageTime": {"minutes": 30, "formatted": "00:30"}},
                           {"status": "AWAITING_APPROVAL", "serviceOrderCount": 2,
                            "averageTime": {"minutes": 780, "formatted": "13:00"}},
                           {"status": "QUOTE_APPROVED", "serviceOrderCount": 2,
                            "averageTime": {"minutes": 10, "formatted": "00:10"}},
                           {"status": "IN_PROGRESS", "serviceOrderCount": 2,
                            "averageTime": {"minutes": 928, "formatted": "15:28"}},
                           {"status": "COMPLETED", "serviceOrderCount": 1,
                            "averageTime": {"minutes": 60, "formatted": "01:00"}}]}
                        """.formatted(alignment, oilChange),
                        JsonCompareMode.STRICT));
    }

    @Test
    void shouldReturnANullAverageForAPeriodWithoutServiceOrders() throws Exception {
        whenAdminRequests("2001-04-10", "2001-04-20")
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                        {"serviceOrderCount": 0, "averageExecutionTime": null, "byService": [], "timeByStatus": []}
                        """,
                        JsonCompareMode.STRICT));
    }

    @Test
    void shouldRejectAPeriodThatStartsAfterItEnds() throws Exception {
        whenAdminRequests("2001-04-20", "2001-04-10")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("metrics.invalidPeriod"));
    }

    @Test
    void shouldRejectAPeriodLongerThan366Days() throws Exception {
        whenAdminRequests("2001-01-01", "2002-01-02")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("metrics.periodTooLong"));
    }

    private ResultActions whenAdminRequests(String from, String to) throws Exception {
        return mockMvc.perform(get(ROUTE)
                .param("from", from)
                .param("to", to)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ADMIN)));
    }

    private String tokenFor(Role... roles) {
        return tokenIssuer.issue(1L, Set.of(roles)).value();
    }

    private static Instant at(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
    }
}
