package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties;
import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties.Limit;
import br.com.arktech.workshop.auth.infrastructure.config.RateLimitProperties.RouteLimit;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class RateLimitFilterTest {

    private static final Duration MINUTE = Duration.ofMinutes(1);

    @Test
    void shouldPreferTheMostSpecificRouteRegardlessOfOrder() throws Exception {
        Map<String, RouteLimit> routes = new LinkedHashMap<>();
        routes.put("admin", new RouteLimit(null, "/admin/**", 5, MINUTE));
        routes.put("createUser", new RouteLimit("POST", "/admin/users", 1, MINUTE));
        RateLimitFilter filter = filter(routes);

        assertThat(status(filter, "POST", "/admin/users")).isEqualTo(200);
        assertThat(status(filter, "POST", "/admin/users")).isEqualTo(429);
        assertThat(status(filter, "GET", "/admin/users")).isEqualTo(200);
    }

    @Test
    void shouldFallBackToTheGeneralLimit() throws Exception {
        RateLimitFilter filter = filter(Map.of("login", new RouteLimit("POST", "/auth/login", 1, MINUTE)));

        assertThat(status(filter, "GET", "/admin/users")).isEqualTo(200);
        assertThat(status(filter, "GET", "/admin/users")).isEqualTo(200);
        assertThat(status(filter, "GET", "/admin/users")).isEqualTo(429);
        assertThat(status(filter, "POST", "/auth/login")).isEqualTo(200);
    }

    @Test
    void shouldNotLimitHealthCheck() throws Exception {
        RateLimitFilter filter = filter(Map.of());

        for (int i = 0; i < 5; i++) {
            assertThat(status(filter, "GET", "/actuator/health/liveness")).isEqualTo(200);
        }
    }

    private static RateLimitFilter filter(Map<String, RouteLimit> routes) {
        RateLimitProperties properties = new RateLimitProperties(new Limit(2, MINUTE), new Limit(2, MINUTE), routes);
        return new RateLimitFilter(properties, (request, response, handler, ex) -> {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return new ModelAndView();
        });
    }

    private static int status(RateLimitFilter filter, String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
